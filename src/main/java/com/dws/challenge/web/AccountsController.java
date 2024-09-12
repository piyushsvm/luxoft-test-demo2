package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.service.AccountsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  /**
   * Handles the transfer of money between two accounts via a POST request
   *
   * This method facilitates the transfer of the specified amount from one account to another.
   * It validates the request parameters, calls the service layer to perform the transfer,
   * and returns an appropriate HTTP response based on the outcome
   *
   * @param accountFromId the ID of the account from which money is being transferred
   * @param accountToId the ID of the account to which money is being transferred
   * @param amount the amount of money to transfer
   *
   */
  @PostMapping(path = "/transfer")
  public ResponseEntity<Object> transferMoney(
          @RequestParam String accountFromId,
          @RequestParam String accountToId,
          @RequestParam BigDecimal amount) {

    log.info("Transferring from {} to {} amount {}", accountFromId, accountToId, amount);

    try {
      accountsService.transferMoney(accountFromId, accountToId, amount);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (IllegalArgumentException | InsufficientFundsException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

}
