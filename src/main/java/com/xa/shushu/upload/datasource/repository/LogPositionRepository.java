package com.xa.shushu.upload.datasource.repository;

import com.xa.shushu.upload.datasource.entity.LogPosition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogPositionRepository extends MongoRepository<LogPosition, String> {
}
