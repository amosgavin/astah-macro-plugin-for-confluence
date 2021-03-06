package com.change_vision.astah.listener;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.atlassian.confluence.event.events.content.attachment.AttachmentCreateEvent;
import com.atlassian.confluence.event.events.content.attachment.AttachmentEvent;
import com.atlassian.confluence.event.events.content.attachment.AttachmentUpdateEvent;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.change_vision.astah.exporter.DiagramExportRunnable;
import com.change_vision.astah.file.AstahBaseDirectory;
import com.change_vision.astah.file.ExportBaseDirectory;
import com.change_vision.astah.util.Util;

public class AttachmentListener implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentListener.class);

    protected EventPublisher eventPublisher;

    private ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor();

    private final ExportBaseDirectory exportBase;

    private final AstahBaseDirectory astahBase;

    private final Util util = new Util();

    public AttachmentListener(BootstrapManager bootstrapManager, EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
        exportBase = new ExportBaseDirectory(bootstrapManager);
        astahBase = new AstahBaseDirectory(bootstrapManager);
        logger.trace("created attachment listener");
    }

    @EventListener
    public void attachmentCreateEvent(AttachmentCreateEvent event) {
        logger.trace("attachmentCreateEvent!!");
        exportDiagramImages(event);
    }

    @EventListener
    public void attachmentUpdateEvent(AttachmentUpdateEvent event) {
        logger.trace("attachmentUpdateEvent!!");
        exportDiagramImages(event);
    }

    private void exportDiagramImages(AttachmentEvent event) {
        logger.trace("attachment event : {}", event);
        boolean updateEvent = (event instanceof AttachmentUpdateEvent);
        List<Attachment> attachments = event.getAttachments();
        for (final Attachment attachment : attachments) {
            logger.info("attachment : {}", attachment.getFileName());
            String extension = attachment.getFileExtension();
            if (needsToExport(updateEvent, attachment) && util.isTargetExtension(extension)) {
                logger.info("start export : {}", attachment.getId());
                DiagramExportRunnable runnable = new DiagramExportRunnable(attachment, astahBase,
                        exportBase);
                scheduledExecutorService.execute(runnable);
                logger.info("end export : {}", attachment.getId());
            }
        }
    }

    private boolean needsToExport(boolean updateEvent, Attachment attachment) {
        return updateEvent || attachment.isNew();
    }

    // Unregister the listener if the plugin is uninstalled or disabled.
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

}