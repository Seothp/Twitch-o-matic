package com.pingwinno.presentation.management.api;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingwinno.application.DateConverter;
import com.pingwinno.application.StorageHelper;
import com.pingwinno.application.twitch.playlist.handler.VodMetadataHelper;
import com.pingwinno.domain.VodDownloader;
import com.pingwinno.domain.sqlite.handlers.SqliteStreamDataHandler;
import com.pingwinno.infrastructure.RecordStatusList;
import com.pingwinno.infrastructure.SettingsProperties;
import com.pingwinno.infrastructure.StreamNotFoundExeption;
import com.pingwinno.infrastructure.enums.StartedBy;
import com.pingwinno.infrastructure.enums.State;
import com.pingwinno.infrastructure.models.*;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Path("/management_api")
public class CRUDApiHandler {
    private org.slf4j.Logger log = LoggerFactory.getLogger(getClass().getName());

    @Path("/add")
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response startRecord(AddDataModel dataModel) {
        Response response;
        StreamExtendedDataModel streamMetadata = null;
        try {
            log.trace("type: {}", dataModel.getType());
            log.trace("value: {}", dataModel.getValue());
            if (dataModel.getType().equals("user")) {
                streamMetadata = VodMetadataHelper.getLastVod(dataModel.getValue());
            } else if (dataModel.getType().equals("vod")) {
                streamMetadata = VodMetadataHelper.getVodMetadata(dataModel.getValue());
            }
            if (streamMetadata != null) {
                VodDownloader vodDownloader = new VodDownloader();
                if (streamMetadata.getVodId() != null) {

                    streamMetadata.setUuid(StorageHelper.getUuidName());
                    StreamExtendedDataModel finalStreamMetadata = streamMetadata;
                    streamMetadata.setSkipMuted(dataModel.isSkipMuted());

                    new RecordStatusList().addStatus
                            (new StatusDataModel(streamMetadata.getVodId(), StartedBy.MANUAL, DateConverter.convert(LocalDateTime.now()),
                                    State.INITIALIZE, streamMetadata.getUuid(), streamMetadata.getUser()));

                    new Thread(() -> {
                        try {
                            vodDownloader.initializeDownload(finalStreamMetadata);
                        } catch (SQLException e) {
                            log.error("DB error {} ",e);
                        }
                    }).start();

                    String startedAt = streamMetadata.getDate();
                    log.info("Record started at:{} ", startedAt);
                    response = Response.accepted().build();
                } else {
                    log.error("Stream {] not found", dataModel.getValue());
                    response = Response.status(Response.Status.NOT_FOUND).build();
                }
            } else {
                response = Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
        } catch (IOException | InterruptedException | SQLException | StreamNotFoundExeption e) {
            response = Response.status(500, e.toString()).build();
            log.error("Can't start record {}", e);
        }
        return response;
    }

    @Path("/validate")
    @POST
    @Produces(MediaType.TEXT_HTML)
    public Response validateRecord(ValidationDataModel dataModel) {
        Response response;
        StreamExtendedDataModel streamMetadata = null;
        try {
            log.trace("vodId: {}", dataModel.getVodId());
            log.trace("uuid: {}", dataModel.getUuid());

            streamMetadata = VodMetadataHelper.getVodMetadata(dataModel.getVodId());

            if (streamMetadata != null) {
                VodDownloader vodDownloader = new VodDownloader();
                if (streamMetadata.getVodId() != null) {

                    streamMetadata.setUuid(dataModel.getUuid());
                    StreamExtendedDataModel finalStreamMetadata = streamMetadata;
                    streamMetadata.setSkipMuted(dataModel.isSkipMuted());

                    new RecordStatusList().addStatus
                            (new StatusDataModel(streamMetadata.getVodId(), StartedBy.VALIDATION, DateConverter.convert(LocalDateTime.now()),
                                    State.INITIALIZE, streamMetadata.getUuid(), streamMetadata.getUser()));

                    new Thread(() -> {
                        try {
                            vodDownloader.initializeDownload(finalStreamMetadata);
                        } catch (SQLException e) {
                            log.error("DB error {} ",e);
                        }
                    }).start();

                    String startedAt = streamMetadata.getDate();
                    log.info("Record started at:{} ", startedAt);
                    response = Response.accepted().build();
                } else {
                    log.error("Stream {] not found", dataModel.getVodId());
                    response = Response.status(Response.Status.NOT_FOUND).build();
                }
            } else {
                response = Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
        } catch (IOException | InterruptedException | SQLException | StreamNotFoundExeption e) {
            response = Response.status(500, e.toString()).build();
            log.error("Can't start record {}", e);
        }
        return response;
    }

    @Path("/{user}/{uuid}")
    @DELETE
    public Response deleteStream(@PathParam("uuid") String uuid, @PathParam("user") String user, @QueryParam("delete_media") String deleteMedia) {

        SqliteStreamDataHandler sqliteHandler = new SqliteStreamDataHandler();
        sqliteHandler.delete(uuid, user);
        log.info("delete stream {}", uuid);
        if (deleteMedia.equals("true")) {
            try {
                Files.delete(Paths.get(SettingsProperties.getRecordedStreamPath() + uuid));
            } catch (IOException e) {
                log.error("can't delete media {] ", e);
                return Response.notModified().build();
            }
        }

        return Response.accepted().build();
    }

    @Path("/update")
    @POST
    public Response updateStream(StreamDataModel dataModel) {
        SqliteStreamDataHandler sqliteStreamDataHandler = new SqliteStreamDataHandler();
        try {
            sqliteStreamDataHandler.update(dataModel);
        } catch (SQLException e) {
            log.error("update failed { }", e);
            return Response.notModified().build();
        }
        return Response.ok().build();

    }

    @Path("/streams/{user}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamsList(@PathParam("user") String user) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new ObjectMapper().writeValueAsString(new SqliteStreamDataHandler().selectAll(user))).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Path("/users")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersList() {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new ObjectMapper().writeValueAsString(SettingsProperties.getUsers())).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
