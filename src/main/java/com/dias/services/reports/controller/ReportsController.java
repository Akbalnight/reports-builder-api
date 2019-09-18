package com.dias.services.reports.controller;

import com.dias.services.core.Details;
import com.dias.services.notifications.NotifificationsData;
import com.dias.services.notifications.interfaces.INotificationsService;
import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.export.ExportFileFormat;
import com.dias.services.reports.model.Report;
import com.dias.services.reports.report.query.QueryDescriptor;
import com.dias.services.reports.report.query.ResultSet;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/reports/analytics")
@Api(tags = "REST API модуля отчетов", description = "Контроллер для работы с отчетами")
public class ReportsController extends AbstractController {

    private static Logger LOG = Logger.getLogger(ReportsController.class.getName());

    private final ReportService reportService;
    private final INotificationsService notificationService;

    @Value("${userId:10}")
    public Long defaultUserId;

    @Autowired
    public ReportsController(ReportService reportService, INotificationsService notificationsService) {
        this.reportService = reportService;
        this.notificationService = notificationsService;
    }

    @ApiOperation(value = "Получение всех отчетов")
    @GetMapping(value = "/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReportDTO> getAll() {
        return reportService.getAllReports();
    }

    @ApiOperation(value = "Получение отчета по id")
    @GetMapping(value = "/reports/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReportDTO getById(@PathVariable Long id) throws Exception {
        return reportService.getReportById(id);
    }

    @ApiOperation(value = "Создание отчета")
    @PostMapping(value = "/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportDTO> create(@RequestBody ReportDTO report) {
        reportService.createReport(report);
        return new ResponseEntity<>(report, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Обновление отчета")
    @PutMapping(value = "/reports/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportDTO> update(@PathVariable Long id, @RequestBody ReportDTO reportDTO) throws Exception {
        ReportDTO originalReport = reportService.getReportById(id);
        String reportTitle = originalReport.getTitle();
        boolean wasFavorite = originalReport.getIsFavorite() != null && originalReport.getIsFavorite();
        boolean wasPublic = originalReport.getIsPublic() != null && originalReport.getIsPublic();
        reportService.merge(originalReport, reportDTO);
        notifyAction(id, reportTitle, NotifificationsData.REPORT_UPDATED);

        //  для проверки изменения атрибута всегда проверяем, что атрибут был передан в DTO объекте
        if (reportDTO.getIsFavorite() != null && !wasFavorite && reportDTO.getIsFavorite()) {
            notifyAction(id, reportTitle, NotifificationsData.REPORT_ADDED_FAVORITE);
        }
        if (reportDTO.getIsPublic() != null && !wasPublic && reportDTO.getIsPublic()) {
            notifyAction(id, reportTitle, NotifificationsData.REPORT_ADDED_PUBLIC);
        }
        if (reportDTO.getIsPublic() != null && wasPublic && !reportDTO.getIsPublic()) {
            notifyAction(id, reportTitle, NotifificationsData.REPORT_ADDED_PRIVATE);
        }
        return new ResponseEntity<>(originalReport, HttpStatus.OK);
    }

    private void notifyAction(Long reportId, String reportTitle, NotifificationsData op) {
        try {
            Long userId = Details.getDetails().getUserId();
            if (userId == null) {
                userId = defaultUserId;
                LOG.info(String.format("Использован userId по умолчанию: %d", defaultUserId));
            }
            if (userId != null) {
                notificationService.sendNotification(
                        op.value(),
                        new String[]{reportTitle},
                        null,
                        Long.toString(reportId),
                        userId.intValue());
            } else {
                LOG.severe(String.format("Отсутствует userId в запросе. Уведомление для операции %d по отчету %s не создано: ", op.value(), reportTitle));
            }
        } catch (Exception e) {
            LOG.severe(String.format("Неизвестная ошибка отправки уведомления: %s", e.getMessage()));
        }
    }

    @ApiOperation(value = "Удаление отчета")
    @DeleteMapping("/reports/{id}")
    public void delete(@PathVariable Long id) throws Exception {
        ReportDTO report = getById(id);
        String reportTitle = report.getTitle();
        reportService.delete(id);
        notifyAction(id, reportTitle, NotifificationsData.REPORT_DELETED);

    }

    @ApiOperation(value = "Предпросмотр")
    @PostMapping(value = "/reports/_preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultSet> preview(@RequestBody QueryDescriptor descriptor) {
        ResultSet rs = reportService.previewByDescriptor(descriptor);
        return new ResponseEntity<>(rs, HttpStatus.OK);
    }

    @ApiOperation(value = "Предпросмотр с итоговой строкой")
    @PostMapping(value = "/reports/_previewWithTotal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultSetWithTotal> previewWithTotal(@RequestBody QueryDescriptor descriptor) {
        ResultSetWithTotal rs = reportService.previewWithTotalByDescriptor(descriptor);
        return new ResponseEntity<>(rs, HttpStatus.OK);
    }

    @ApiOperation(value = "Синхронный запуск отчета")
    @PostMapping(value = "/reports/{id}/_execute", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultSet> syncExetcuteReport(
            @PathVariable Long id,
            @RequestParam(required = false) Long limit,
            @RequestParam(required = false) Long offset) throws Exception {
        Report report = reportService.getById(id);
        ResultSet rs = reportService.executeReport(report, limit, offset);
        return new ResponseEntity<>(rs, HttpStatus.OK);
    }

    @ApiOperation(value = "Экспорт отчета")
    @PostMapping(value = "/reports/{id}/_export")
    public ResponseEntity syncExportReport(
            @PathVariable Long id,
            @RequestParam(value = "format", required = false) ExportFileFormat format) throws Exception {
        return syncExportReportWithFileName(id, format, null);
    }

    @ApiOperation(value = "Экспорт отчета")
    @GetMapping(value = "/reports/{id}/_export")
    public ResponseEntity syncExportReportGet(
            @PathVariable Long id,
            @RequestParam(value = "format", required = false) ExportFileFormat format) throws Exception {
        return syncExportReportWithFileName(id, format, null);
    }

    @ApiOperation(value = "Экспорт отчета с указанным именем файла (режим совместимости с IE)")
    @GetMapping(value = "/reports/{id}/_export/{fileName}")
    public ResponseEntity syncExportReportWithFileNameGet(
            @PathVariable Long id,
            @RequestParam(value = "format", required = false) ExportFileFormat format,
            @RequestParam(value = "fileName", required = false) String fileName) throws Exception {
        return syncExportReportWithFileName(id, format, fileName);
    }

    @ApiOperation(value = "Экспорт отчета с указанным именем файла (режим совместимости с IE)")
    @PostMapping(value = "/reports/{id}/_export/{fileName}")
    public ResponseEntity syncExportReportWithFileName(
            @PathVariable Long id,
            @RequestParam(value = "format", required = false) ExportFileFormat format,
            @RequestParam(value = "fileName", required = false) String fileName) throws Exception {

        Report report = reportService.getById(id);
        String reportTitle = report.getTitle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExportFileFormat fileFormat = format != null ? format : ExportFileFormat.XLSX;
        ResponseEntity<Resource> resourceResponseEntity;
        String resultFile = (fileName != null ? fileName : "report") + "." + fileFormat.name().toLowerCase();
        if (fileFormat == ExportFileFormat.XLSX) {
            reportService.exportToExcel(report, out);
            resourceResponseEntity = downloadExcel(resultFile, out.toByteArray());
        } else {
            reportService.exportToPdf(report, out);
            resourceResponseEntity = downloadPdf(resultFile, out.toByteArray());
        }
        if (fileName != null) {
            resourceResponseEntity.getHeaders().remove(HttpHeaders.CONTENT_DISPOSITION);
        }
        notifyAction(id, reportTitle, NotifificationsData.REPORT_EXPORTED);
        return resourceResponseEntity;
    }


    @ApiOperation(value = "Синхронный запуск отчета с итоговой строкой")
    @PostMapping(value = "/reports/{id}/_executeWithTotal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultSetWithTotal> syncExetcuteWithTotalReport(
            @PathVariable Long id,
            @RequestParam(required = false) Long limit,
            @RequestParam(required = false) Long offset) throws Exception {
        Report report = reportService.getById(id);
        ResultSetWithTotal rs = reportService.executeWithTotalReport(report, limit, offset);
        return new ResponseEntity<>(rs, HttpStatus.OK);
    }


    @ApiOperation(value = "Асинхронный запуск отчета")
    @PostMapping(value = "/reports/{id}/_executeAsync", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity exetcuteReport(@PathVariable Long id) throws Exception {
        Report report = reportService.getById(id);
        reportService.asyncExecuteReport(report);
        return new ResponseEntity(HttpStatus.OK);
    }


}
