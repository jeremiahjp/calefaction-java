package com.jp.calefaction.repository;

import com.jp.calefaction.entity.Users;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends ReactiveCrudRepository<Users, String> {}
