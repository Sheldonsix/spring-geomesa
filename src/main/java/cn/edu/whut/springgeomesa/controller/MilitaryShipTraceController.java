package cn.edu.whut.springgeomesa.controller;

import cn.edu.whut.springgeomesa.config.impl.MilitaryShipTraceDataConfig;
import cn.edu.whut.springgeomesa.pojo.dto.GeomesaDTO;
import cn.edu.whut.springgeomesa.service.IMilitaryShipTraceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * @ClassName MilitaryShipTraceController
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/25 17:29
 * @Version 1.0
 **/
@RestController
@CrossOrigin
@RequestMapping(value = "/militaryship")
@Api("data")
public class MilitaryShipTraceController {
    private final static Logger logger = LoggerFactory.getLogger(InfraredRayController.class);

    @Autowired
    private IMilitaryShipTraceService iMilitaryShipTraceService;

    @Autowired
    private MilitaryShipTraceDataConfig militaryShipTraceDataConfig;

    @Autowired
    private ValueOperations<String, Object> redisString;

    /**
     * @return java.lang.String
     * @author sheldon
     * @date 2022/5/25
     * @description 插入军船轨迹数据
     * @param: catalogName
     **/
    @ApiOperation(value = "写入军船轨迹数据", notes = "写入 HBase 数据源")
    @ApiImplicitParam(name = "catalogName", value = "HBase 数据源", required = true, dataType = "String", paramType = "path")
    @GetMapping(value = "/insert/{catalogName}")
    public String insertMilitaryShip(@PathVariable("catalogName") String catalogName) {
        logger.info(catalogName + "插入军船轨迹数据……");
        GeomesaDTO geomesaDTO = new GeomesaDTO();
        geomesaDTO.setParams(new HashMap<String, String>() {
            {
                put("hbase.catalog", catalogName);
                put("hbase.zookeepers", "master,slave1,slave2");
            }
        });
        geomesaDTO.setData(militaryShipTraceDataConfig);
        Boolean result = iMilitaryShipTraceService.insertMilitaryShipTraceData(geomesaDTO.getParams());
        if (result == true) {
            return "插入军船轨迹数据成功";
        } else {
            return "插入军船轨迹数据失败";
        }
    }

    @ApiOperation(value = "查询军船轨迹数据", notes = "查询军船轨迹数据")
    @ApiImplicitParam(name = "catalogName", value = "HBase 数据源", required = true, dataType = "String", paramType = "path")
    @GetMapping(value = "/query/{catalogName}")
    public String queryMilitaryShipTrace(@PathVariable("catalogName") String catalogName, @RequestParam(value = "voyage", defaultValue = "72", required = false) String voyage) {
        logger.info(catalogName + "查询军船轨迹数据……");
        String queryStr = "dtg DURING 2022-01-13T00:00:00.000Z/2022-01-30T00:00:00.000Z AND Voyage = '144' AND Speed < 12.3 AND Speed > 12.1";
        if (redisString.get(queryStr) != null) {
            logger.info("已在 Redis 数据库中找到……");
            String militaryShipTraceGeoJson = (String) redisString.get(queryStr);
            return militaryShipTraceGeoJson;
        } else {
            GeomesaDTO geomesaDTO = new GeomesaDTO();
            geomesaDTO.setParams(new HashMap<String, String>() {
                {
                    put("hbase.catalog", catalogName);
                    put("hbase.zookeepers", "master,slave1,slave2");
                }
            });
            geomesaDTO.setData(militaryShipTraceDataConfig);
            String militaryShipTraceGeoJson = iMilitaryShipTraceService.attributeQuery(geomesaDTO.getParams(), queryStr);
            if (militaryShipTraceGeoJson != null) {
                return militaryShipTraceGeoJson;
            } else {
                return "查询军船轨迹数据错误";
            }
        }
    }

    @ApiOperation(value = "删除军船轨迹数据", notes = "删除 HBase 数据源")
    @ApiImplicitParam(name = "catalogName", value = "HBase 数据源", required = true, dataType = "String", paramType = "path")
    @GetMapping(value = "/delete/{catalogName}")
    public String deleteMilitaryShipTrace(@PathVariable("catalogName") String catalogName) {
        logger.info(catalogName + "删除军船轨迹数据……");
        GeomesaDTO geomesaDTO = new GeomesaDTO();
        geomesaDTO.setParams(new HashMap<String, String>() {
            {
                put("hbase.catalog", catalogName);
                put("hbase.zookeepers", "master,slave1,slave2");
            }
        });
        geomesaDTO.setData(militaryShipTraceDataConfig);
        Boolean status = iMilitaryShipTraceService.deleteMilitaryShipTraceDatastore(geomesaDTO.getParams());
        if (status == true) {
            return "删除军船轨迹数据成功";
        } else {
            return "删除军船轨迹数据成功";
        }
    }
}
