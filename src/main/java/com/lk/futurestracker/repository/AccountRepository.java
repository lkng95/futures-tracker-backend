package com.lk.futurestracker.repository;

import com.lk.futurestracker.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
