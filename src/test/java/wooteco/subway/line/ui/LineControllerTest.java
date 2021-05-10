package wooteco.subway.line.ui;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import wooteco.subway.line.domain.Line;
import wooteco.subway.line.domain.LineRepository;
import wooteco.subway.line.domain.Section;
import wooteco.subway.line.domain.Sections;
import wooteco.subway.line.service.LineService;
import wooteco.subway.line.ui.dto.LineCreateRequest;
import wooteco.subway.line.ui.dto.LineModifyRequest;
import wooteco.subway.line.ui.dto.LineResponse;
import wooteco.subway.station.domain.Station;
import wooteco.subway.station.domain.StationRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("/truncate.sql")
class LineControllerTest {

    @Autowired
    private LineService lineService;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    private StationRepository stationRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @DisplayName("새로운 노선을 생성한다.")
    @Test
    void createNewline_createNewLineFromUserInputs() {
        //given
        Station station1 = setDummyStation("봉천역");
        Station station2 = setDummyStation("신림역");
        final LineCreateRequest request = new LineCreateRequest("bg-red-600", "신분당선", station1.getId(), station2.getId(), 10);

        //when
        final ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .body(request)
                .post("/lines")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Location", "/lines/1")
                .extract();

        //then
        final LineResponse lineResponse = response.as(LineResponse.class);
        assertThat(request.getName()).isEqualTo(lineResponse.getName());
        assertThat(request.getColor()).isEqualTo(lineResponse.getColor());
    }

    @DisplayName("모든 노선을 조회한다.")
    @Test
    void allLines() {
        //given
        Line newLine = setDummyLine("강남역", "양재역", 10, "신분당선", "bg-red-600");
        Line twoLine = setDummyLine("봉천역", "신림역", 10, "2호선", "bg-red-500");

        //when
        final ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .extract();

        //then
        LineResponse[] responses = response.as(LineResponse[].class);
        assertThat(responses)
                .hasSize(2)
                .extracting(LineResponse::getName)
                .contains("신분당선", "2호선");

        assertThat(responses)
                .hasSize(2)
                .extracting(LineResponse::getColor)
                .contains("bg-red-600", "bg-red-500");
    }

    @DisplayName("노선을 검색한다")
    @Test
    void findById_findLineById() {
        //given
        Line line = setDummyLine("강남역", "양재역", 10, "신분당선", "bg-red-600");

        //when
        final ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + line.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .extract();

        //then
        LineResponse lineResponse = response.as(LineResponse.class);
        assertThat(line.getId()).isEqualTo(lineResponse.getId());
        assertThat(line.getName()).isEqualTo(lineResponse.getName());
        assertThat(line.getColor()).isEqualTo(lineResponse.getColor());
    }

    @DisplayName("노선이 없다면 400에러 발생")
    @Test
    void findById_canNotFindLineById() {
        //given

        //when
        //then
        RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/1")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("노션을 수정한다.")
    @Test
    void modifyById_modifyLineFromUserInputs() {
        //given
        Line line = setDummyLine("강남역", "양재역", 10, "신분당선", "bg-red-600");

        //when
        final ExtractableResponse<Response> response = RestAssured
                .given().log().all()
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .body(new LineModifyRequest("bg-red-600", "구분당선"))
                .put("/lines/" + line.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .extract();

        //then
        final Line updatedLine = lineRepository.findById(line.getId());
        assertThat(updatedLine.getName()).isEqualTo("구분당선");
    }

    @DisplayName("노션을 삭제한다.")
    @Test
    void deleteById_deleteLineFromUserInputs() {
        //given
        Line line = setDummyLine("강남역", "양재역", 10, "신분당선", "bg-red-600");

        //when
        RestAssured
                .given().log().all()
                .accept(MediaType.ALL_VALUE)
                .when()
                .delete("/lines/" + line.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        //then
        assertThatThrownBy(() -> {
            lineRepository.findById(line.getId());
        }, "", EmptyResultDataAccessException.class);

    }

    @Test
    @DisplayName("노선에 포함된 역들을 순서대로 조회한다.")
    void showSectionsInLine() {
        //given
        Line line = setDummyLine("강남역", "양재역", 10, "신분당선", "bg-red-600");
        final List<Station> stations = lineService.getStations(line.getId());
        LineResponse testResponse = new LineResponse(line, stations);

        //when
        LineResponse resultResponse = RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + line.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .assertThat()
                .extract()
                .as(LineResponse.class);

        //then
        assertThat(resultResponse.getStations()).hasSize(2);
        assertThat(resultResponse).isEqualTo(testResponse);
    }


    private Station setDummyStation(String stationName) {
        Station station = new Station(stationName);
        return stationRepository.save(station);
    }

    private Line setDummyLine(String upStationName, String downStationName, int distance, String lineName, String lineColor) {
        Station upStation = new Station(upStationName);
        Station downStation = new Station(downStationName);

        Station savedUpStation = stationRepository.save(upStation);
        Station savedDownStation = stationRepository.save(downStation);

        Sections sections = new Sections(
                Collections.singletonList(
                        new Section(savedUpStation.getId(), savedDownStation.getId(), distance)
                )
        );

        return lineRepository.save(new Line(lineName, lineColor, sections));
    }
}