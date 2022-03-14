package ca.bc.gov.educ.api.dataconversion.reader;

import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.service.conv.DataConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PenUpdatesPartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PenUpdatesPartitioner.class);

    private final DataConversionService dataConversionService;

    public PenUpdatesPartitioner(DataConversionService dataConversionService) {
        this.dataConversionService = dataConversionService;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext executionContext = new ExecutionContext();
            ConversionStudentSummaryDTO summaryDTO = new ConversionStudentSummaryDTO();
            List<String> data = loadPenNumbers(i, 2000);
            executionContext.put("data", data);
            summaryDTO.setReadCount(data.size());
            executionContext.put("summary", summaryDTO);
            String key = "partition" + i;
            map.put(key, executionContext);
        }
        return map;
    }

    private List<String> loadPenNumbers(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("studNo").ascending());
        return dataConversionService.loadAllTraxStudentsForPenUpdate(pageable).stream().map(r -> r.getStudNo()).collect(Collectors.toList());
    }
}
