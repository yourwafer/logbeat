package com.xa.shushu.upload.datasource.controller;

import com.xa.shushu.upload.datasource.entity.LogPosition;
import com.xa.shushu.upload.datasource.entity.MysqlPosition;
import com.xa.shushu.upload.datasource.service.FileProcessService;
import com.xa.shushu.upload.datasource.service.MysqlProcessService;
import com.xa.shushu.upload.datasource.service.report.Report;
import com.xa.shushu.upload.datasource.service.report.ReportUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("_report")
public class ReportController {

    private final FileProcessService fileProcessService;

    private MysqlProcessService mysqlProcessService;

    public ReportController(FileProcessService fileProcessService, MysqlProcessService mysqlProcessService) {
        this.fileProcessService = fileProcessService;
        this.mysqlProcessService = mysqlProcessService;
    }

    @GetMapping("_state")
    public Report getReport() {
        return ReportUtils.get();
    }

    @GetMapping("_logposition")
    public List<LogPosition> logs() {
        return fileProcessService.getLogs();
    }

    @GetMapping("_mysqlposition")
    public List<MysqlPosition> mysqls() {
        return mysqlProcessService.getMysqls();
    }


    @GetMapping("_health")
    public boolean health() {
        return fileProcessService.isRunning() && mysqlProcessService.isRunning();
    }

    @GetMapping("_health/file")
    public boolean fileRunning() {
        return fileProcessService.isRunning();
    }

    @GetMapping("_health/mysql")
    public boolean mysqlRunning() {
        return mysqlProcessService.isRunning();
    }
}
