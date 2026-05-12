package com.today.fridge.ingredient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RemoteImageStagingResponse(@JsonProperty("pending_file_id") Long pendingFileId) {}
