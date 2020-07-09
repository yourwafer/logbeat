package com.xa.shushu.upload.datasource.repository;

import com.xa.shushu.upload.datasource.entity.LogPosition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogPositionRepository extends CrudRepository<LogPosition, String> {
}
