function extract_container_id(tag, timestamp, record)
    local path = record["log_file"]
    if path ~= nil then
        local id = path:match("/([0-9a-f][0-9a-f]+)/[^/]+$")
        if id ~= nil then
            record["container_id"] = id:sub(1, 12)
        end
    end
    return 1, timestamp, record
end
