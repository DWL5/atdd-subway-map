package wooteco.subway.line.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wooteco.subway.line.domain.Line;
import wooteco.subway.line.domain.LineRepository;
import wooteco.subway.line.domain.Section;
import wooteco.subway.line.repository.SectionDao;
import wooteco.subway.station.domain.Station;
import wooteco.subway.station.domain.StationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {

    private final LineRepository lineRepository;
    private final StationRepository stationRepository;

    @Autowired
    public LineService(final LineRepository lineRepository, final StationRepository stationRepository) {
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
    }

    public Line save(Line line) {
        return lineRepository.save(line);
    }

    public List<Station> getStations(Long lineId) {
        Line line = lineRepository.findById(lineId);
        List<Section> sectionList = line.getSections().toList();
        List<Station> stations = sectionList.stream()
                .map(section -> stationRepository.findById(section.getUpStationId()))
                .collect(Collectors.toList());

        Long lastStationId = sectionList.get(sectionList.size() -1).getDownStationId();
        stations.add(stationRepository.findById(lastStationId));

        return stations;

    }

    public List<Line> allLines() {
        return lineRepository.allLines();
    }

    public Line findById(final Long id) {
        return lineRepository.findById(id);
    }

    public void update(final Line line) {
        lineRepository.update(line);
    }

    public void deleteById(final Long id) {
        lineRepository.deleteById(id);
    }
}
