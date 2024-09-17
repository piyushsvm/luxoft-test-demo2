package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  private final EmailNotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, EmailNotificationService notificationService) {

    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }



  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /**
   * Transfers the specified amount of money from one account to another.
   *
   * @param accountFromId the ID of the account from which the moneyto be transferred
   * @param accountToId the ID of the account to which the money is to be transferred
   * @param amount the amount to transfer
   *
   * @throws IllegalArgumentException if one of the accounts does not exist
   * @throws InsufficientFundsException if the source account does not have enough funds to complete the transfer
   */
  public void transferMoney(String accountFromId, String accountToId, BigDecimal amount) throws InsufficientFundsException {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transfer amount must be greater than zero");
    }
    Account accountFrom = accountsRepository.getAccount(accountFromId);
    Account accountTo = accountsRepository.getAccount(accountToId);

    if (accountFrom == null || accountTo == null) {
      throw new IllegalArgumentException("One of the accounts does not exist");
    }

    Account firstLock;
    Account secondLock;
    //done to ensure locking only happens in a consistent and predefined order i.e. by comaring
    //eg in case Account id 1  makes transfer to Account id 2 by comparing synchronization always happens in consisten order
    if (accountFromId.compareTo(accountToId) < 0) {
      firstLock = accountFrom;
      secondLock = accountTo;
    } else {
      firstLock = accountTo;
      secondLock = accountFrom;
    }

    synchronized (firstLock) {
      synchronized (secondLock) {
        if (accountFrom.getBalance().compareTo(amount) < 0) {
          throw new InsufficientFundsException("Insufficient balance in account " + accountFromId);
        }

        accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
        accountTo.setBalance(accountTo.getBalance().add(amount));

        accountsRepository.updateAccount(accountFrom);
        accountsRepository.updateAccount(accountTo);

        // After successful transfer, send notifications
        notificationService.notifyAboutTransfer(accountFrom, "Transferred " + amount + " to " + accountTo.getAccountId());
        notificationService.notifyAboutTransfer(accountTo, "Received " + amount + " from " + accountFrom.getAccountId());
      }
    }
  }



}
