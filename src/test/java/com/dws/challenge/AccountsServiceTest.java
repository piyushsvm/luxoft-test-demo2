package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @BeforeEach
  void setUp() {
    this.accountsService.getAccountsRepository().clearAccounts();
  }
  @Test
  void transferMoneySuccessful() {

    Account accountFrom = new Account("Id-101", new BigDecimal("1000"));
    Account accountTo = new Account("Id-102", new BigDecimal("500"));
    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);


    this.accountsService.transferMoney("Id-101", "Id-102", new BigDecimal("200"));


    assertThat(this.accountsService.getAccount("Id-101").getBalance()).isEqualByComparingTo("800");
    assertThat(this.accountsService.getAccount("Id-102").getBalance()).isEqualByComparingTo("700");
  }

  @Test
  void transferMoneyInsufficientFunds() {

    Account accountFrom = new Account("Id-1", new BigDecimal("100"));
    Account accountTo = new Account("Id-2", new BigDecimal("500"));
    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);

    try {
      this.accountsService.transferMoney("Id-1", "Id-2", new BigDecimal("200"));
      fail("Should have thrown InsufficientFundsException");
    } catch (InsufficientFundsException ex) {
      assertThat(ex.getMessage()).isEqualTo("Insufficient balance in account Id-1");
    }

    // Ensure balances remain unchanged
    assertThat(this.accountsService.getAccount("Id-1").getBalance()).isEqualByComparingTo("100");
    assertThat(this.accountsService.getAccount("Id-2").getBalance()).isEqualByComparingTo("500");
  }


}
