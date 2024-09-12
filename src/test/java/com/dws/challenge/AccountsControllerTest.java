package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }



  @Test
  void transferMoneySuccessful() throws Exception {
    // Create two accounts
    this.mockMvc.perform(post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"Id-1\",\"balance\":1000}"))
            .andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"Id-2\",\"balance\":500}"))
            .andExpect(status().isCreated());

    // Perform the transfer
    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .param("accountFromId", "Id-1")
                    .param("accountToId", "Id-2")
                    .param("amount", "200"))
            .andExpect(status().isOk());

    // Verify balances after transfer
    assertThat(accountsService.getAccount("Id-1").getBalance()).isEqualByComparingTo("800");
    assertThat(accountsService.getAccount("Id-2").getBalance()).isEqualByComparingTo("700");
  }

  @Test
  void transferMoneyInsufficientFunds() throws Exception {
    // Create two accounts
    this.mockMvc.perform(post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"Id-1\",\"balance\":100}"))
            .andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"Id-2\",\"balance\":500}"))
            .andExpect(status().isCreated());

    // Attempt to transfer more money than available
    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .param("accountFromId", "Id-1")
                    .param("accountToId", "Id-2")
                    .param("amount", "200"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferMoneyInvalidAccount() throws Exception {
    // Create only one account
    this.mockMvc.perform(post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"Id-1\",\"balance\":1000}"))
            .andExpect(status().isCreated());

    // Attempt to transfer to a non-existent account
    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .param("accountFromId", "Id-1")
                    .param("accountToId", "NonExistingAccount")
                    .param("amount", "100"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferMoneyNegativeAmount() throws Exception {
    // Create two accounts
    this.mockMvc.perform(post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"Id-1\",\"balance\":1000}"))
            .andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"Id-2\",\"balance\":500}"))
            .andExpect(status().isCreated());

    // Attempt to transfer a negative amount
    this.mockMvc.perform(post("/v1/accounts/transfer")
                    .param("accountFromId", "Id-1")
                    .param("accountToId", "Id-2")
                    .param("amount", "-100"))
            .andExpect(status().isBadRequest());
  }


}
