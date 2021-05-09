package wooteco.subway.station.ui.dto;

import java.beans.ConstructorProperties;

public class StationRequest {
    private String name;

    @ConstructorProperties({"name"})
    public StationRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
