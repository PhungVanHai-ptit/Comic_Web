package com.haiphung.comic_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.haiphung.comic_web.entity.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer>{

    Optional<Role> findByRoleName(String roleName) ;
}
