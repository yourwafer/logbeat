package com.xa.shushu.upload.datasource.repository;

import com.xa.shushu.upload.datasource.entity.MysqlPosition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MysqlPositionRepository extends CrudRepository<MysqlPosition, String> {
}
