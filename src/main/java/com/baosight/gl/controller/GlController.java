package com.baosight.gl.controller;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.baosight.gl.domain.CastIronItemValue;
import com.baosight.gl.excel.mode.BlastFurnaceMode;
import com.baosight.gl.mapper.db1.ProcessMapper;
import com.baosight.gl.service.gl.GlService;
import com.baosight.gl.service.gl.ProcessService;
import com.baosight.gl.utils.NumberFormatUtils;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@SuppressWarnings("all")
public class GlController {
	@RequestMapping("/")
	public String initLoad() {
		return "gl-service启动成功！";
	}

	@Autowired
	GlService glService;

	@Autowired
	ProcessService processService;

	@Autowired
	ProcessMapper processMapper;

	/**
	 * 根据cutoffId、开始时间、结束时间，查询时间段内的ResultId集合
	 * 
	 * @param cutoffId
	 * @param startTime
	 * @param endTime
	 */
	@CrossOrigin
	@PostMapping("/queryResultIdbyTimes")
	public String queryResultIdbyTimes(String cutoffId, String startTime, String endTime) {
		try {
			// 声明retMap集合
			Map retMap = new LinkedHashMap();
			// 声明resultIdRetList集合
			List<Integer> resultIdRetList = new ArrayList<>();
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("cutoffId", cutoffId);
			paramsMap.put("startTime", startTime);
			paramsMap.put("endTime", endTime);
			paramsMap.put("orderFetch", "order by resultId asc");
			// 根据参数查询ResultId集合
			List<HashMap> resultIdList = processMapper.queryCutoffResult(paramsMap);
			// 判断时间段内是否存在resultId
			if (resultIdList.size() == 0) {
				// 处理startTimeValue、endTimeValue、resultIdRetList到retMap集合
				retMap.put("startTime", startTime);
				retMap.put("endTime", endTime);
				retMap.put("size", resultIdRetList.size());
				retMap.put("interval", "1min");
				retMap.put("resultIds", resultIdRetList);
				// 返回
				return JSON.toJSONString(retMap);
			}
			// 遍历resultIdList集合
			for (int i = 0; i < resultIdList.size(); i++) {
				Integer resultId = Integer.valueOf(resultIdList.get(i).get("RESULTID").toString());
				resultIdRetList.add(resultId);
			}
			// 获取开始时间
			Object startTimeValue = resultIdList.get(0).get("FORMATCLOCK");
			// 获取结束时间
			Object endTimeValue = resultIdList.get(resultIdList.size() - 1).get("FORMATCLOCK");
			// 处理startTimeValue、endTimeValue、resultIdRetList到retMap集合
			retMap.put("startTime", startTimeValue);
			retMap.put("endTime", endTimeValue);
			retMap.put("size", resultIdRetList.size());
			retMap.put("interval", "1min");
			retMap.put("resultIds", resultIdRetList);
			// 返回
			return JSON.toJSONString(retMap);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询出铁数据项
	 * 
	 * @remark1：传参resultId：进行历史回放查询功能
	 * @remark2：传参resultId：查询和resultId时间最近的出铁数据项
	 * @remark3：不传参resultId：查询最新的出铁数据项
	 * 
	 * @param resultId
	 */
	@CrossOrigin
	@PostMapping("/queryCastIron")
	public String queryCastIron(Integer resultId) {
		try {
			// 声明参数
			HashMap paramsMap = resultId != null ? processService.getTapNoByResultId(resultId, 6) : new HashMap<>();
			// 声明出铁数据返回值
			Map<String, String> castIronRetMap = new HashMap<>();
			// 查询出铁数据
			List<CastIronItemValue> castIronList = glService.queryCastIron(paramsMap);
			// 转换list为map集合
			Map<String, String> castIronMap = castIronList.stream().collect(Collectors.toMap(CastIronItemValue::getKeyName, CastIronItemValue::getValue));
			// 处理出铁数据：温度
			processService.handleData("PRC_CC_TAP_TEMP", "CastIronTemperature", castIronMap, castIronRetMap);
			// 处理出铁数据：Si
			processService.handleData("PRC_CC_PIG_COMPONENT_SI", "CastIronSi", castIronMap, castIronRetMap);
			// 处理出铁数据：S
			processService.handleData("PRC_CC_PIG_COMPONENT_S", "CastIronS", castIronMap, castIronRetMap);
			// 处理出铁数据：CaO
			processService.handleData("PRC_CC_SLAG_COMPONENT_CAO", "CastIronCaO", castIronMap, castIronRetMap);
			// 处理出铁数据：SiO2
			processService.handleData("PRC_CC_SLAG_COMPONENT_SIO2", "CastIronSiO2", castIronMap, castIronRetMap);
			// 返回
			return JSON.toJSONString(castIronRetMap);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询4个出铁信号点，value=0：代表出铁中,value=2：代表出铁结束
	 * 
	 * @remark1：出铁信号点表为实时更新表，不存在历史回放查询
	 */
	@CrossOrigin
	@PostMapping("/queryCastIronTag")
	public String queryCastIronTag() {
		try {
			// 查询出铁信号点
			List<HashMap> castIronTagList = glService.queryCastIronTag();
			// 判空并重新赋值
			castIronTagList = castIronTagList == null ? new ArrayList<>() : castIronTagList;
			// 返回
			return JSON.toJSONString(castIronTagList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询38个风口信号点，value=1：代表风口启用,value=0：代表风口不启用 
	 * 
	 * @remark1：风口信号点表为实时更新表，不存在历史回放查询
	 */
	@CrossOrigin
	@PostMapping("/queryTuyere")
	public String queryTuyere() {
		try {
			// 查询风口数据项
			List<HashMap> TuyereList = glService.queryTuyereTag();
			// 判空并重新赋值
			TuyereList = TuyereList == null ? new ArrayList<>() : TuyereList;
			// 返回
			return JSON.toJSONString(TuyereList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询指标、操炉参数、料线数据项
	 * 
	 * @remark1：传参resultId：进行历史回放查询功能
	 * @remark2：传参resultId：查询resultId对应的指标、操炉参数、料线数据项
	 * @remark3：不传参resultId：查询最新的指标、操炉参数、料线数据项
	 * @remark4：产量、焦比、煤比、燃料比、料速：小时级数据，特殊处理resultId
	 * 
	 * @param resultId
	 */
	@CrossOrigin
	@PostMapping("/queryFurnaceMaterial")
	public String queryFurnaceMaterial(Integer resultId) {
		try {
			// 声明参数
			HashMap paramsMap = resultId != null ? processService.getCutOff3ResultIdByResultId(resultId, 2) : new HashMap<>();
			paramsMap.put("rows", 1);
//			// 指标、操炉参数、料线：T_CUTOFF_RESULT_CAL_2产量数据
//			HashMap FurnaceMaterialMap_1 = glService.queryFurnaceMaterial1(paramsMap);
//			FurnaceMaterialMap_1 = FurnaceMaterialMap_1 == null ? new HashMap<>() : FurnaceMaterialMap_1;
			// 指标、操炉参数、料线：T_CUTOFF_RESULT_AVG_4
			HashMap FurnaceMaterialMap_2 = glService.queryFurnaceMaterial2(paramsMap);
			FurnaceMaterialMap_2 = FurnaceMaterialMap_2 == null ? new HashMap<>() : FurnaceMaterialMap_2;
			// 指标、操炉参数、料线：T_CUTOFF_RESULT_AVG_3
			HashMap FurnaceMaterialMap_3 = glService.queryFurnaceMaterial3(paramsMap);
			FurnaceMaterialMap_3 = FurnaceMaterialMap_3 == null ? new HashMap<>() : FurnaceMaterialMap_3;
			// 指标、操炉参数、料线：T_CUTOFF_RESULT_CAL_21
			HashMap FurnaceMaterialMap_4 = glService.queryFurnaceMaterial4(paramsMap);
			FurnaceMaterialMap_4 = FurnaceMaterialMap_4 == null ? new HashMap<>() : FurnaceMaterialMap_4;
			HashMap FurnaceMaterialMap_5 = glService.queryFurnaceMaterial5(paramsMap);
			FurnaceMaterialMap_5 = FurnaceMaterialMap_5 == null ? new HashMap<>() : FurnaceMaterialMap_5;
			HashMap FurnaceMaterialMap_8 = glService.queryFurnaceMaterial8(paramsMap);
			FurnaceMaterialMap_8 = FurnaceMaterialMap_8 == null ? new HashMap<>() : FurnaceMaterialMap_8;
			// 声明FurnaceMaterialMap集合
			HashMap FurnaceMaterialMap = new HashMap<>();
			// 处理指标、操炉参数、料线数据项到FurnaceMaterialMap集合
//			FurnaceMaterialMap.putAll(FurnaceMaterialMap_1);
			FurnaceMaterialMap.putAll(FurnaceMaterialMap_2);
			FurnaceMaterialMap.putAll(FurnaceMaterialMap_3);
			FurnaceMaterialMap.putAll(FurnaceMaterialMap_4);
			FurnaceMaterialMap.putAll(FurnaceMaterialMap_5);
			FurnaceMaterialMap.putAll(FurnaceMaterialMap_8);
			// 声明参数key集合：26个数据项
			List<String> FurnaceMaterialKeyList = new ArrayList<>();
			// 产量
			FurnaceMaterialKeyList.add("CG6_HSD_TFE_ABS");
			// 入炉焦比
			FurnaceMaterialKeyList.add("CG6_HSD_CCR_002");
			// 入炉煤比
			FurnaceMaterialKeyList.add("CG6_HSD_CRR_002");
			// 入炉燃料比
			FurnaceMaterialKeyList.add("CG6_HSD_FUR_002");
			// 入炉焦比
			FurnaceMaterialKeyList.add("JIAOBI");
			// 入炉煤比
			FurnaceMaterialKeyList.add("MEIBI");
			// 入炉燃料比
			FurnaceMaterialKeyList.add("RANLIAOBI");
			// co
			FurnaceMaterialKeyList.add("RG5_BFG_TOP_GCO");
			// 全压差
			FurnaceMaterialKeyList.add("RG5_BFP_DIF_000");
			// 顶温
			FurnaceMaterialKeyList.add("RG5_BFT_TOP_AVG");
			// 风量
			FurnaceMaterialKeyList.add("RG5_BFF_COL_001");
			// 喷煤
			FurnaceMaterialKeyList.add("RG5_BFF_COA_000");
			// 富氧
			FurnaceMaterialKeyList.add("RG5_HSF_000_GO2");
			// 热风压力
			FurnaceMaterialKeyList.add("RG5_BFP_HOT_001");
			// 理论燃烧
			FurnaceMaterialKeyList.add("RG5_BFT_FIR_000");
			// 炉腹煤气
			FurnaceMaterialKeyList.add("RG5_BFN_LFM_101");
			// 铁水
			FurnaceMaterialKeyList.add("CG5_MDT_TSP_001");
			// co2
			FurnaceMaterialKeyList.add("RG5_BFG_TOP_CO2");
			// ηCO氧化碳利用率
			FurnaceMaterialKeyList.add("RG5_BFN_RCO_000");
			// cct5
			FurnaceMaterialKeyList.add("RG5_CRT_CCT_005");
			// 炉顶压力
			FurnaceMaterialKeyList.add("RG5_BFP_TOP_AVG");
			// 鼓风动能
			FurnaceMaterialKeyList.add("RG5_BFN_GFD_001");
			// 热风温度
			FurnaceMaterialKeyList.add("RG5_HST_000_BLA");
			// k
			FurnaceMaterialKeyList.add("RG5_BFN_K00_001");
			// si
			FurnaceMaterialKeyList.add("CG5_MDN_TSN_0SI");
// CCT
			FurnaceMaterialKeyList.add("CCT");
			//gufeng
			FurnaceMaterialKeyList.add("gufeng");
			//pianchi
			FurnaceMaterialKeyList.add("pianchi");
			//liaosu
			FurnaceMaterialKeyList.add("liaosu");
			//bianyuan
			FurnaceMaterialKeyList.add("bianyuan");
			//xianliao
			FurnaceMaterialKeyList.add("xianliao");
			// 富氧率
			FurnaceMaterialKeyList.add("RG5_BFR_GO2_000");
			//冷风温度
			FurnaceMaterialKeyList.add("RG5_BFT_COL_001");
			//送风流量
			FurnaceMaterialKeyList.add("RG5_BFF_WIN_000");
			//冷风压力
			FurnaceMaterialKeyList.add("RG5_HSP_000_COL");
//煤气
			FurnaceMaterialKeyList.add("RG5_BFG_TOP_CH4");
			//透气性指数
			FurnaceMaterialKeyList.add("RG5_BFO_00K_001");
			// 声明FurnaceMaterialValueMap集合
			Map FurnaceMaterialValueMap = new HashMap<>();
			// 遍历FurnaceMaterialKeyList集合
			for (String FurnaceMaterialKey : FurnaceMaterialKeyList) {
				processService.handleData(FurnaceMaterialKey, FurnaceMaterialKey, FurnaceMaterialMap, FurnaceMaterialValueMap);
			}
			// 返回
			return JSON.toJSONString(FurnaceMaterialValueMap);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询热负荷数据项
	 * 
	 * @remark1：传参resultId：进行历史回放查询功能
	 * @remark2：传参resultId：查询resultId对应的热负荷数据项
	 * @remark3：不传参resultId：查询最新的热负荷数据项
	 * 
	 * @param resultId
	 */
	@CrossOrigin
	@PostMapping("/queryThermalLoad")
	public String queryThermalLoad(String resultId) {
		try {
			// 声明BlastFurnaceList集合
			List<BlastFurnaceMode> BlastFurnaceList = new ArrayList<>();
			// 声明BlastFurnaceModeMap集合
			Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = new HashMap<>();
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", 1);
			paramsMap.put("resultId", resultId);
			/**
			 * 根据参数：获取热负荷数据项
			 */
			// 获取热负荷对应的excel数据项
			BlastFurnaceList = processService.readBlastFurnaceExcel("热负荷数据项_1.xlsx");
			// 声明tableNamesList集合
			List<String> tableNamesList = new ArrayList<String>();
			// 添加数据项到tableNamesList集合
			tableNamesList.add("T_CUTOFF_RESULT_AVG_56");
			tableNamesList.add("T_CUTOFF_RESULT_AVG_57");
			// BlastFurnaceList根据table字段分组
			BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
			// 声明ThermalLoadTempMap集合
			HashMap ThermalLoadMapAll = new HashMap<>();
			// 遍历tableNamesList集合
			for (int i = 0; i < tableNamesList.size(); i++) {
				// 获取表名
				String tableName = tableNamesList.get(i);
				// 处理表名到paramsMap集合
				paramsMap.put("tableName", tableName);
				// 获取查询字段
				processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "field");
				// 查询热负荷数据项
				HashMap ThermalLoadMap = glService.queryThermalLoadTemp(paramsMap).get(0);
				// 处理热负荷温度数据项到ThermalLoadTempMap集合
				ThermalLoadMapAll.putAll(ThermalLoadMap);
			}
			// 获取fieldStr参数
//			processService.getFieldStr(BlastFurnaceModeMap.get("T_CUTOFF_RESULT_AVG_2"), paramsMap, "field");
//			processService.getFieldStr(BlastFurnaceModeMap.get("T_CUTOFF_RESULT_AVG_3"), paramsMap, "field");
			// 查询热负荷数据项
//			HashMap ThermalLoadMap = glService.queryThermalLoad(paramsMap).get(0);
			// 根据：数据库数据项、excel数据项，处理热负荷数据格式
			List<HashMap> FormatThermalLoadList = processService.formatBlastFurnaceData(ThermalLoadMapAll, BlastFurnaceList,resultId);
			/**
			 * 根据参数：获取热负荷温度数据项
			 */
			// 获取热负荷温度对应的excel数据项
			BlastFurnaceList = processService.readBlastFurnaceExcel("热负荷温度数据项_1.xlsx");
			// BlastFurnaceList根据table字段分组
			BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
//			// 声明tableNamesList集合
			List<String> tableNamesListforTemp = new ArrayList<String>();
			// 添加数据项到tableNamesList集合
			tableNamesListforTemp.add("T_CUTOFF_RESULT_AVG_54");
			tableNamesListforTemp.add("T_CUTOFF_RESULT_AVG_55");
//			tableNamesListforTemp.add("T_CUTOFF_RESULT_AVG_2");
//			tableNamesListforTemp.add("T_CUTOFF_RESULT_AVG_10");
			// 声明ThermalLoadTempMap集合
			HashMap ThermalLoadTempMap = new HashMap<>();
			// 遍历tableNamesList集合
			for (int i = 0; i < tableNamesListforTemp.size(); i++) {
				// 获取表名
				String tableName = tableNamesListforTemp.get(i);
				// 处理表名到paramsMap集合
				paramsMap.put("tableName", tableName);
				// 获取查询字段
				processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "field");
				// 查询热负荷温度数据项
				HashMap ThermalLoadTempValueMap = glService.queryThermalLoadTemp(paramsMap).get(0);
				// 处理热负荷温度数据项到ThermalLoadTempMap集合
				ThermalLoadTempMap.putAll(ThermalLoadTempValueMap);
			}
			// 根据：数据库数据项、excel数据项，处理热负荷温度数据格式
			List<HashMap> FormatThermalLoadTempList = processService.formatBlastFurnaceData(ThermalLoadTempMap, BlastFurnaceList,resultId);
			// 声明ThermalLoadTempValueHashMap集合
			LinkedHashMap ThermalLoadTempValueHashMap = new LinkedHashMap<>();
			// 处理FormatThermalLoadTempList集合数据项到ThermalLoadTempValueHashMap集合
			processService.dealFormatData(FormatThermalLoadTempList, ThermalLoadTempValueHashMap, "put1");
			// 处理ThermalLoadTempValueHashMap集合数据项到FormatThermalLoadList集合
			processService.dealFormatData(FormatThermalLoadList, ThermalLoadTempValueHashMap, "get1");
//			// 根据time：查询T_CUTOFF_RESULT表时间
//			Long timeStamp = processService.queryResultTimeStamp(1, resultId,"resultId");
//			HashMap timesParamsMap = new HashMap<>();
//			// 处理timeStamp到ThermalLoadRetMap集合
//			timesParamsMap.put("timeStamp", timeStamp);
//			FormatThermalLoadList.add(timesParamsMap);
			// 返回
			return JSON.toJSONString(FormatThermalLoadList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询热负荷温度数据项
	 * 
	 * @remark1：传参resultId：进行历史回放查询功能
	 * @remark2：传参resultId：查询resultId对应的热负荷数温度数据项
	 * @remark3：不传参resultId：查询最新的热负荷温度数据项
	 * @param resultId
	 */
	@CrossOrigin
	@PostMapping("/queryThermalLoadTemp")
	public String queryThermalLoadTemp(String resultId) {
		try {
			// 获取热负荷温度对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热负荷温度数据项_1.xlsx");
			// BlastFurnaceList根据table字段分组
			Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", 1);
			paramsMap.put("resultId", resultId);
			// 声明tableNamesList集合
			List<String> tableNamesList = new ArrayList<String>();
			// 添加数据项到tableNamesList集合
			tableNamesList.add("T_CUTOFF_RESULT_AVG_54");
			tableNamesList.add("T_CUTOFF_RESULT_AVG_55");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_2");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_10");
			// 声明ThermalLoadTempMap集合
			HashMap ThermalLoadTempMap = new HashMap<>();
			// 遍历tableNamesList集合
			for (int i = 0; i < tableNamesList.size(); i++) {
				// 获取表名
				String tableName = tableNamesList.get(i);
				// 处理表名到paramsMap集合
				paramsMap.put("tableName", tableName);
				// 获取查询字段
				processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "field");
				// 查询热负荷温度数据项
				HashMap ThermalLoadTempValueMap = glService.queryThermalLoadTemp(paramsMap).get(0);
				// 处理热负荷温度数据项到ThermalLoadTempMap集合
				ThermalLoadTempMap.putAll(ThermalLoadTempValueMap);
			}
			// 根据：数据库数据项、excel数据项，处理热负荷温度数据格式
			List<HashMap> FormatBlastFurnaceList = processService.formatBlastFurnaceData(ThermalLoadTempMap, BlastFurnaceList,resultId);
			// 返回
			return JSON.toJSONString(FormatBlastFurnaceList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}
//	/**
//	 * 查询热负荷历史趋势图数据项
//	 *
//	 * @remark1：传参rows：从当前时间查询rows行热负荷数据项
//	 * @param rows
//	 */
//	@CrossOrigin
//	@PostMapping("/queryThermalLoadHistory")
//	public String queryThermalLoadHistory(@RequestParam(defaultValue = "60", required = false) Integer rows, String time) {
//		try {
//			// 声明ThermalLoadRetMap集合
//			Map<Object, Object> ThermalLoadRetMap = new LinkedHashMap<>();
//			// 获取热负荷对应的excel数据项
//			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热负荷数据项.xlsx");
//			// BlastFurnaceList根据table字段分组
//			Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
//			// 设置参数
//			HashMap paramsMap = new HashMap<>();
//			paramsMap.put("rows", rows);
//			paramsMap.put("time", time);
//			// 获取fieldStr参数
//			processService.getFieldStr(BlastFurnaceModeMap.get("T_CUTOFF_RESULT_AVG_2"), paramsMap, "field");
//			// 查询热负荷数据项
//			List<HashMap> ThermalLoadList = glService.queryThermalLoad(paramsMap);
//			// 判断是否存在历史热负荷数据项 || 热负荷数据项长度是否大于rows
//			if (ThermalLoadList.size() == 0 || ThermalLoadList.size() < rows) {
//				return JSON.toJSONString(ThermalLoadRetMap);
//			}
//			// 根据time：查询T_CUTOFF_RESULT表时间
//			Long timeStamp = processService.queryResultTimeStamp(1, time);
//			// 处理timeStamp到ThermalLoadRetMap集合
//			// ThermalLoadRetMap.put("timeStamp", timeStamp);
//			// 声明热负荷参数
//			HashMap blastFurnaceParamsMap = new HashMap<>();
//			blastFurnaceParamsMap.put("blastFurnaceList", BlastFurnaceList);
//			blastFurnaceParamsMap.put("dataList", ThermalLoadList);
//			blastFurnaceParamsMap.put("rows", rows);
//			blastFurnaceParamsMap.put("type", 3);
//			// 获取热负荷
//			LinkedHashMap ThermalLoadDealMap = processService.dealBlastFurnaceHistory(blastFurnaceParamsMap);
//			// 处理ThermalLoadDealMap到ThermalLoadRetMap集合
//			ThermalLoadRetMap.putAll(ThermalLoadDealMap);
//			// 获取ThermalLoadTempRetMap集合
//			LinkedHashMap ThermalLoadTempRetMap = queryThermalLoadTempHistory(rows, time);
//			// 根据ThermalLoadRetMap、ThermalLoadTempRetMap集合处理数据项
//			// 声明 ValueLinkedHashMap集合
//			LinkedHashMap ValueLinkedHashMap = new LinkedHashMap<>();
//			// 遍历ThermalLoadRetMap集合
//			for (Object key : ThermalLoadRetMap.keySet()) {
//				String[] keyArrays = key.toString().split("@");
//				String dealKey = keyArrays[1] + "@" + keyArrays[2];
//				//
//				List valueList = new ArrayList<>();
//				valueList.add(ThermalLoadRetMap.get(key));
//				valueList.add(ThermalLoadTempRetMap.get(dealKey));
//				//
//				ValueLinkedHashMap.put(keyArrays[0] + "@" + keyArrays[1], valueList);
//			}
//			// 返回
//			return JSON.toJSONString(ValueLinkedHashMap);
//		} catch (Exception e) {
//			// 返回错误码
//			return "500";
//		}
//	}

	/**
	 * 查询热负荷历史趋势图数据项
	 * 
	 * @remark1：传参rows：从当前时间查询rows行热负荷数据项
	 * @param rows
	 */
	@CrossOrigin
	@PostMapping("/queryThermalLoadHistory")
	public String queryThermalLoadHistory(@RequestParam(defaultValue = "60", required = false) Integer rows, String time) {
		try {
// 声明ThermalLoadTempRetMap集合
			LinkedHashMap<Object, Object> ThermalLoadTempRetMap = new LinkedHashMap<>();
			// 获取热负荷温度对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热负荷数据项_1.xlsx");
			// BlastFurnaceList根据table字段分组
			Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", rows);
			paramsMap.put("time", time);
			// 声明tableNamesList集合
			List<String> tableNamesList = new ArrayList<String>();
			// 添加数据项到tableNamesList集合
			tableNamesList.add("T_CUTOFF_RESULT_AVG_56");
			tableNamesList.add("T_CUTOFF_RESULT_AVG_57");
			// 声明ThermalLoadTempAllList集合
			List<HashMap> ThermalLoadTempAllList = new ArrayList<>();
			// 遍历tableNamesList集合
			for (int i = 0; i < tableNamesList.size(); i++) {
				// 获取表名
				String tableName = tableNamesList.get(i);
				// 处理表名到paramsMap集合
				paramsMap.put("tableName", tableName);
				// 获取查询字段
				processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "field");
				// 查询热负荷历史数据项
				List<HashMap> ThermalLoadTempList = glService.queryThermalLoadHis(paramsMap);
				// 判断是否存在历史热负荷温度数据项 || 热负荷温度数据项长度是否大于rows
				if (ThermalLoadTempList.size() == 0 || ThermalLoadTempList.size() < rows) {
					return JSON.toJSONString(ThermalLoadTempRetMap);
				}
				// 处理ThermalLoadTempList到ThermalLoadTempAllList集合
				ThermalLoadTempAllList.addAll(ThermalLoadTempList);
			}
			// 声明ThermalLoadTempList集合
			List<HashMap> ThermalLoadTempList = new ArrayList<>();
			// 遍历热负荷温度数据项
			for (int i = 0; i < rows; i++) {
				// 声明ThermalLoadTempMap集合
				HashMap ThermalLoadTempMap = new HashMap<>();
				// 处理热负荷温度数据项到ThermalLoadTempMap集合
				ThermalLoadTempMap.putAll(ThermalLoadTempAllList.subList(0, rows).get(i));
				ThermalLoadTempMap.putAll(ThermalLoadTempAllList.subList(rows, rows*2).get(i));
//				ThermalLoadTempMap.putAll(ThermalLoadTempAllList.subList(120, 180).get(i));
				// 处理ThermalLoadTempMap集合到ThermalLoadTempList集合
				ThermalLoadTempList.add(ThermalLoadTempMap);
			}
			// 根据time：查询T_CUTOFF_RESULT表时间
			Long timeStamp = processService.queryResultTimeStamp(1, time);
			// 处理timeStamp到ThermalLoadTempRetMap集合
			// ThermalLoadTempRetMap.put("timeStamp", timeStamp);
			// 声明热负荷温度参数
			HashMap blastFurnaceParamsMap = new HashMap<>();
			blastFurnaceParamsMap.put("blastFurnaceList", BlastFurnaceList);
			blastFurnaceParamsMap.put("dataList", ThermalLoadTempList);
			blastFurnaceParamsMap.put("rows", rows);
			blastFurnaceParamsMap.put("type", 1);
			// 获取热负荷温度
			LinkedHashMap ThermalLoadTempDealMap = processService.dealBlastFurnaceHistory(blastFurnaceParamsMap);
			// 处理ThermalLoadTempDealMap到ThermalLoadTempRetMap集合
			ThermalLoadTempRetMap.putAll(ThermalLoadTempDealMap);
			// 声明dealThermalLoadTempRetMap集合
//			LinkedHashMap dealThermalLoadTempRetMap = new LinkedHashMap<>();
//			// 遍历ThermalLoadTempRetMap集合
//			for (Object key : ThermalLoadTempRetMap.keySet()) {
//				String[] keyArrays = key.toString().split("@");
//				String dealKey = keyArrays[1] + "@" + keyArrays[2];
//				dealThermalLoadTempRetMap.put(dealKey, ThermalLoadTempRetMap.get(key));
//			}
			// 返回
			return JSON.toJSONString(ThermalLoadTempRetMap);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询热负荷温度历史趋势图数据项
	 * 
	 * @remark1：传参rows：从当前时间查询rows行热负荷温度数据项
	 * @param rows
	 */
	@CrossOrigin
	@PostMapping("/queryThermalLoadTempHistory")
//	public LinkedHashMap queryThermalLoadTempHistory(Integer rows, String time) throws Exception {
		public String queryThermalLoadTempHistory(Integer rows, String time) throws Exception {
		// 声明ThermalLoadTempRetMap集合
		LinkedHashMap<Object, Object> ThermalLoadTempRetMap = new LinkedHashMap<>();
		// 获取热负荷温度对应的excel数据项
		List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热负荷温度数据项_1.xlsx");
		// BlastFurnaceList根据table字段分组
		Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
		// 设置参数
		HashMap paramsMap = new HashMap<>();
		paramsMap.put("rows", rows);
		paramsMap.put("time", time);
		// 声明tableNamesList集合
		List<String> tableNamesList = new ArrayList<String>();
		// 添加数据项到tableNamesList集合
		tableNamesList.add("T_CUTOFF_RESULT_AVG_54");
		tableNamesList.add("T_CUTOFF_RESULT_AVG_55");
//		tableNamesList.add("T_CUTOFF_RESULT_AVG_10");
		// 声明ThermalLoadTempAllList集合
		List<HashMap> ThermalLoadTempAllList = new ArrayList<>();
		// 遍历tableNamesList集合
		for (int i = 0; i < tableNamesList.size(); i++) {
			// 获取表名
			String tableName = tableNamesList.get(i);
			// 处理表名到paramsMap集合
			paramsMap.put("tableName", tableName);
			// 获取查询字段
			processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "field");
			// 查询热负荷温度历史数据项
			List<HashMap> ThermalLoadTempList = glService.queryThermalLoadHis(paramsMap);
			// 判断是否存在历史热负荷温度数据项 || 热负荷温度数据项长度是否大于rows
			if (ThermalLoadTempList.size() == 0 || ThermalLoadTempList.size() < rows) {
				return JSON.toJSONString(ThermalLoadTempRetMap);
			}
			// 处理ThermalLoadTempList到ThermalLoadTempAllList集合
			ThermalLoadTempAllList.addAll(ThermalLoadTempList);
		}
		// 声明ThermalLoadTempList集合
		List<HashMap> ThermalLoadTempList = new ArrayList<>();
		// 遍历热负荷温度数据项
		for (int i = 0; i < rows; i++) {
			// 声明ThermalLoadTempMap集合
			HashMap ThermalLoadTempMap = new HashMap<>();
			// 处理热负荷温度数据项到ThermalLoadTempMap集合
			ThermalLoadTempMap.putAll(ThermalLoadTempAllList.subList(0, rows).get(i));
			ThermalLoadTempMap.putAll(ThermalLoadTempAllList.subList(rows, rows*2).get(i));
//			ThermalLoadTempMap.putAll(ThermalLoadTempAllList.subList(120, 180).get(i));
			// 处理ThermalLoadTempMap集合到ThermalLoadTempList集合
			ThermalLoadTempList.add(ThermalLoadTempMap);
		}
		// 根据time：查询T_CUTOFF_RESULT表时间
		Long timeStamp = processService.queryResultTimeStamp(1, time);
		// 处理timeStamp到ThermalLoadTempRetMap集合
		// ThermalLoadTempRetMap.put("timeStamp", timeStamp);
		// 声明热负荷温度参数
		HashMap blastFurnaceParamsMap = new HashMap<>();
		blastFurnaceParamsMap.put("blastFurnaceList", BlastFurnaceList);
		blastFurnaceParamsMap.put("dataList", ThermalLoadTempList);
		blastFurnaceParamsMap.put("rows", rows);
		blastFurnaceParamsMap.put("type", 1);
		// 获取热负荷温度
		LinkedHashMap ThermalLoadTempDealMap = processService.dealBlastFurnaceHistory(blastFurnaceParamsMap);
		// 处理ThermalLoadTempDealMap到ThermalLoadTempRetMap集合
		ThermalLoadTempRetMap.putAll(ThermalLoadTempDealMap);
		// 声明dealThermalLoadTempRetMap集合
//		LinkedHashMap dealThermalLoadTempRetMap = new LinkedHashMap<>();
//		// 遍历ThermalLoadTempRetMap集合
//		for (Object key : ThermalLoadTempRetMap.keySet()) {
//			String[] keyArrays = key.toString().split("@");
//			String dealKey = keyArrays[1] + "@" + keyArrays[2];
//			dealThermalLoadTempRetMap.put(dealKey, ThermalLoadTempRetMap.get(key));
//		}
		// 返回
		return JSON.toJSONString(ThermalLoadTempRetMap);
	}

	/**
	 * 查询(热电偶)等温线数据项
	 * 
	 * @remark1：传参resultId：进行历史回放查询功能
	 * @remark2：传参resultId：查询resultId对应的(热电偶)等温线数据项
	 * @remark3：不传参resultId：查询最新的(热电偶)等温线数据项
	 * @param resultId
	 */
	@CrossOrigin
	@PostMapping("/queryThermocouple")
	public String queryThermocouple(String resultId) {
		try {
			// 获取(热电偶)等温线对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热电偶数据项.xlsx");
			// BlastFurnaceList根据table字段分组
//			Stream API   集合中的对象按照某个属性进行分组 Collectors.groupingBy 方法是一个收集器（Collector），它将流中的元素根据指定的分类函数进行分组
//			collect() 方法接收一个 java.util.stream.Collector 对象，该对象定义了如何进行收集操作
//			可以将 Stream 中的元素收集（Collect）起来，生成一个新的数据结构
			Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", 1);
			paramsMap.put("isotherm", "isotherm");
			paramsMap.put("resultId", resultId);
			// 声明tableNamesList集合
			List<String> tableNamesList = new ArrayList<String>();
			// 添加数据项到tableNamesList集合
			tableNamesList.add("T_CUTOFF_RESULT_AVG_1");
			tableNamesList.add("T_CUTOFF_RESULT_AVG_2");
			tableNamesList.add("T_CUTOFF_RESULT_AVG_3");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_9");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_10");
			// 声明ThermocoupleMap集合
			HashMap ThermocoupleMap = new HashMap<>();
			// 遍历tableNamesList集合
			for (int i = 0; i < tableNamesList.size(); i++) {
				// 获取表名
				String tableName = tableNamesList.get(i);
				// 处理表名到paramsMap集合
				paramsMap.put("tableName", tableName);
				// 获取查询字段
				processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "field");
				// 查询(热电偶)等温线数据项
				HashMap ThermocoupleValueMap = glService.queryThermocouple(paramsMap).get(0);
				// 处理热电偶数据项到ThermocoupleMap集合
				ThermocoupleMap.putAll(ThermocoupleValueMap);
			}
			// 根据：数据库数据项、excel数据项，处理(热电偶)等温线数据格式
			List<HashMap> FormatBlastFurnaceList = processService.formatBlastFurnaceData(ThermocoupleMap, BlastFurnaceList,resultId);
//			// 根据time：查询T_CUTOFF_RESULT表时间
//			Long timeStamp = processService.queryResultTimeStamp(1, resultId,"resultId");
//			HashMap timesParamsMap = new HashMap<>();
//			// 处理timeStamp到ThermalLoadRetMap集合
//			timesParamsMap.put("timeStamp", timeStamp);
//			FormatBlastFurnaceList.add(timesParamsMap);
			// 返回
			return JSON.toJSONString(FormatBlastFurnaceList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询(热电偶)等温线历史趋势图数据项
	 * 
	 * @remark1：传参rows：从当前时间查询rows行(热电偶)等温线历史数据项
	 * @param rows
	 */
	@CrossOrigin
	@PostMapping("/queryThermocoupleHistory")
	public String queryThermocoupleHistory(@RequestParam(defaultValue = "60", required = false) Integer rows, String time) {
		try {
			// 声明ThermocoupleRetMap集合
			Map<Object, Object> ThermocoupleRetMap = new LinkedHashMap<>();
			// 获取(热电偶)等温线对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热电偶数据项.xlsx");
			// BlastFurnaceList根据table字段分组
			Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", rows);
			paramsMap.put("time", time);
			paramsMap.put("isotherm", "isotherm");
			// 声明tableNamesList集合
			List<String> tableNamesList = new ArrayList<String>();
			// 添加数据项到tableNamesList集合
			tableNamesList.add("T_CUTOFF_RESULT_AVG_1");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_7");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_8");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_9");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_10");
			// 声明ThermocoupleAllList集合
			List<HashMap> ThermocoupleAllList = new ArrayList<>();
			// 遍历tableNamesList集合
			for (int i = 0; i < tableNamesList.size(); i++) {
				// 获取表名
				String tableName = tableNamesList.get(i);
				// 处理表名到paramsMap集合
				paramsMap.put("tableName", tableName);
				// 获取查询字段
				processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "field");
				// 查询(热电偶)等温线数据项
				List<HashMap> ThermocoupleList = glService.queryThermocoupleHis(paramsMap);
				// 判断是否存在历史(热电偶)等温线数据项 || (热电偶)等温线数据项长度是否大于rows
				if (ThermocoupleList.size() == 0 || ThermocoupleList.size() < rows) {
					return JSON.toJSONString(ThermocoupleRetMap);
				}
				// 处理ThermocoupleList到ThermocoupleAllList集合
				ThermocoupleAllList.addAll(ThermocoupleList);
			}
			// 声明ThermocoupleList集合
			List<HashMap> ThermocoupleList = new ArrayList<>();
			// 遍历(热电偶)等温线数据项
			//每个表数据查询60条,表的数量*60=总共的数据量,把总的数据量,汇总到要求的60条数据内   即同一个时间id对应多个或者一个表数据
			for (int i = 0; i < rows; i++) {
				// 声明ThermocoupleMap集合
				HashMap ThermocoupleMap = new HashMap<>();
				// 处理(热电偶)等温线数据项到ThermocoupleMap集合
				ThermocoupleMap.putAll(ThermocoupleAllList.subList(0, rows).get(i));
//				ThermocoupleMap.putAll(ThermocoupleAllList.subList(0, 60).get(i));
//				ThermocoupleMap.putAll(ThermocoupleAllList.subList(60, 120).get(i));
//				ThermocoupleMap.putAll(ThermocoupleAllList.subList(120, 180).get(i));
//				ThermocoupleMap.putAll(ThermocoupleAllList.subList(180, 240).get(i));
				// 处理ThermocoupleMap集合到ThermocoupleList集合
				ThermocoupleList.add(ThermocoupleMap);
			}
			// 根据time：查询T_CUTOFF_RESULT表时间
			Long timeStamp = processService.queryResultTimeStamp(1, time);
			// 处理timeStamp到ThermalLoadRetMap集合
			ThermocoupleRetMap.put("timeStamp", timeStamp);
			// 声明(热电偶)等温线参数
			HashMap blastFurnaceParamsMap = new HashMap<>();
			blastFurnaceParamsMap.put("blastFurnaceList", BlastFurnaceList);
			blastFurnaceParamsMap.put("dataList", ThermocoupleList);
			blastFurnaceParamsMap.put("rows", rows);
			blastFurnaceParamsMap.put("type", 1);
			// 获取(热电偶)等温线
			LinkedHashMap ThermocoupleDealMap = processService.dealBlastFurnaceHistory(blastFurnaceParamsMap);
			// 处理ThermocoupleDealMap到ThermocoupleRetMap集合
			ThermocoupleRetMap.putAll(ThermocoupleDealMap);
			// 返回
			return JSON.toJSONString(ThermocoupleRetMap);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 热力图数据项：计算热电偶的数据项差值
	 */
	@CrossOrigin
	@PostMapping("/queryHeatMap")
	public String queryHeatMap(String resultId) {
		try {
			// 获取热电偶对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热电偶数据项.xlsx");
			// BlastFurnaceList根据table字段分组
			Map<String, List<BlastFurnaceMode>> BlastFurnaceModeMap = BlastFurnaceList.stream().collect(Collectors.groupingBy(BlastFurnaceMode::getTable));
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", 25);
			paramsMap.put("heatMap", "heatMap");
			paramsMap.put("resultId", resultId);
			// 获取热力图对应的HeatMapResultIdHashMap集合
			HashMap<String, Object> HeatMapResultIdHashMap = processService.getHeatMapResultId(paramsMap);
			// 判断HeatMapResultIdHashMap集合
			if (HeatMapResultIdHashMap == null) {
				return "";
			}
			// 获取HeatMapResultIdList集合
			List<Integer> HeatMapResultIdList = (List<Integer>) HeatMapResultIdHashMap.get("HeatMapResultId");
			// 获取HeatMapResultIdStr、HeatMapResultIdBeforeStr字符串 当前的一小时和一天前的一小时的数据(默认按照1分钟一条)
			String HeatMapResultIdStr = HeatMapResultIdList.subList(0, 60).toString().replace("[", "(").replace("]", ")");
			String HeatMapResultIdBeforeStr = HeatMapResultIdList.subList(1440, 1500).toString().replace("[", "(").replace("]", ")");
			// 声明tableNamesList集合
			List<String> tableNamesList = new ArrayList<String>();
			// 添加数据项到tableNamesList集合
			tableNamesList.add("T_CUTOFF_RESULT_AVG_1");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_7");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_8");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_9");
//			tableNamesList.add("T_CUTOFF_RESULT_AVG_10");
			// 声明HeatMapHashMap集合
			LinkedHashMap HeatMapHashMap = new LinkedHashMap<>();
			// 声明HeatMapAvgHashMap集合
			LinkedHashMap HeatMapAvgHashMap = new LinkedHashMap<>();
			// 声明HeatMapAvgBeforeHashMap集合
			LinkedHashMap HeatMapAvgBeforeHashMap = new LinkedHashMap<>();
			// 遍历tableNamesList集合
			for (int i = 0; i < tableNamesList.size(); i++) {
				// 获取表名
				String tableName = tableNamesList.get(i);
				// 处理表名到paramsMap集合
				paramsMap.put("tableName", tableName);
				// 获取查询字段
				processService.getFieldStr(BlastFurnaceModeMap.get(tableName), paramsMap, "average");
				// 处理HeatMapResultIdStr到paramsMap集合
				paramsMap.put("HeatMapResultIdStr", HeatMapResultIdStr);
				// 声明HeatMapAvgHashMap集合
				HashMap HeatMapAvgValueHashMap = glService.queryThermocouple(paramsMap).get(0);
				// 处理HeatMapResultIdBeforeStr到paramsMap集合
				paramsMap.put("HeatMapResultIdStr", HeatMapResultIdBeforeStr);
				// 声明HeatMapAvgBeforeHashMap集合
				HashMap HeatMapAvgBeforeValueHashMap = glService.queryThermocouple(paramsMap).get(0);
				// 声明HeatMapAvgList集合
				List<HashMap> HeatMapAvgList = new ArrayList<>();
				// 处理HeatMapAvgValueHashMap、HeatMapAvgBeforeValueHashMap到HeatMapAvgList集合
				HeatMapAvgList.add(HeatMapAvgValueHashMap);
				HeatMapAvgList.add(HeatMapAvgBeforeValueHashMap);
				// 根据HeatMapAvgHashMap、HeatMapAvgBeforeHashMap计算差值
				HashMap HeatMapValueHashMap = processService.getHeatMapValueDiffByList(HeatMapAvgList).get(0);
				// 处理HeatMapValueHashMap集合到HeatMapHashMap集合
				HeatMapHashMap.putAll(HeatMapValueHashMap);
				// 处理HeatMapAvgValueHashMap集合到HeatMapAvgHashMap集合
				HeatMapAvgHashMap.putAll(HeatMapAvgValueHashMap);
				// 处理HeatMapAvgBeforeValueHashMap集合到HeatMapAvgBeforeHashMap集合
				HeatMapAvgBeforeHashMap.putAll(HeatMapAvgBeforeValueHashMap);
			}
			// 根据：数据库数据项、excel数据项，处理热电偶数据格式(热力图负值不进行处理)
			List<HashMap> FormatBlastFurnaceList = processService.formatBlastFurnaceData(HeatMapHashMap, BlastFurnaceList,resultId,"heatMap");
			// 处理HeatMapAvgHashMap集合数据项到FormatBlastFurnaceList集合
			processService.dealFormatData(FormatBlastFurnaceList, HeatMapAvgHashMap, "get2");
//			// 根据time：查询T_CUTOFF_RESULT表时间
//			Long timeStamp = processService.queryResultTimeStamp(1, resultId,"resultId");
//			HashMap timesParamsMap = new HashMap<>();
//			// 处理timeStamp到ThermalLoadRetMap集合
//			timesParamsMap.put("timeStamp", timeStamp);
//			FormatBlastFurnaceList.add(timesParamsMap);
			// 返回
			return JSON.toJSONString(FormatBlastFurnaceList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 热力图历史趋势图数据项：计算热电偶指定的数据项差值
	 */
	@CrossOrigin
	@PostMapping("/queryHeatMapHistory")
	public String queryHeatMapHistory(@RequestParam(defaultValue = "25", required = false) Integer rows, String time, String field) {
		try {
			// 声明HeatMapRetMap集合
			Map<Object, Object> HeatMapRetMap = new LinkedHashMap<>();
			// 获取热力图对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("热电偶数据项.xlsx");
			// 根据field获取：BlastFurnaceByFieldList集合
			List<BlastFurnaceMode> BlastFurnaceByFieldList = BlastFurnaceList.stream().filter(o -> o.getField().equals(field)).collect(Collectors.toList());
			// 根据BlastFurnaceByFieldList：获取table
			String BlastFurnaceTable = BlastFurnaceByFieldList.get(0).getTable();
			// 根据BlastFurnaceByFieldList：获取field
			String BlastFurnaceField = BlastFurnaceByFieldList.get(0).getField();
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", rows);
			paramsMap.put("time", time);
			paramsMap.put("heatMap", "heatMap");
			// 获取热力图对应的resultId集合字符串
			HashMap<String, Object> HeatMapResultIdHashMap = processService.getHeatMapResultId(paramsMap);
			String HeatMapResultIdStr = HeatMapResultIdHashMap.get("HeatMapResultIdStr").toString();
			// 设置参数
			paramsMap = new HashMap<>();
			paramsMap.put("BlastFurnaceField", BlastFurnaceField);
			paramsMap.put("BlastFurnaceTable", BlastFurnaceTable);
			paramsMap.put("HeatMapResultIdStr", HeatMapResultIdStr);
			List<HashMap> HeatMapList = glService.queryHeatMapHistory(paramsMap);
			// 声明HeatMapValueList集合
			List<Object> HeatMapValueList = new ArrayList<>();
			// 遍历HeatMapList集合
			for (int startIndex = 0; startIndex < HeatMapList.size(); startIndex += 60) {
				ArrayList HeatMapValue = new ArrayList<>();
				// 每隔60条数据(一个小时)：处理成ValueList集合
				List<HashMap> ValueList = HeatMapList.subList(startIndex, startIndex + 60);
				// 根据ValueList集合：获取一个小时的平均值
				Double Value = ValueList.stream().mapToDouble(e -> Double.parseDouble(e.get(field).toString())).summaryStatistics().getAverage();
				// 格式化平均值：保留两位小数
				Value = NumberFormatUtils.formatDouble(Value);
				// 处理格式化value：到HeatMapValueList集合
				//为热负荷值添加时间
				HeatMapValue.add(ValueList.get(0).get("CLOCK"));
				HeatMapValue.add(Value);
				HeatMapValueList.add(HeatMapValue);
			}
			// 根据time：查询T_CUTOFF_RESULT表时间
			Long timeStamp = processService.queryResultTimeStamp(1, time);
			// 处理timeStamp到HeatMapRetMap集合
			HeatMapRetMap.put("timeStamp", timeStamp);
			// 处理field、HeatMapValueList到HeatMapRetMap集合
			HeatMapRetMap.put(field, HeatMapValueList);
			// 返回
			return JSON.toJSONString(HeatMapRetMap);
		} catch (Exception e) {
			// 返回错误码
			return JSON.toJSONString(new LinkedHashMap<>());
		}
	}

	/**
	 * 查询压力数据项：炉身静压、冷却板供水压力、热风压力、炉顶压力
	 * 
	 * @remark1：传参resultId：进行历史回放查询功能
	 * @remark2：传参resultId：查询resultId对应的压力数据项
	 * @remark3：不传参resultId：查询最新的压力数据项
	 * @param resultId
	 */
	@CrossOrigin
	@PostMapping("/queryPressure")
	public String queryPressure(String resultId) {
		try {
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", 1);
			paramsMap.put("resultId", resultId);
			// 查询炉身静压数据项：T_CUTOFF_RESULT_AVG_1
			List<HashMap> PressureList_1 = glService.queryStaticPressure(paramsMap);
			HashMap PressureMap_1 = PressureList_1.get(0);
			// 查询冷却板供水压力数据项：T_CUTOFF_RESULT_AVG_1
//			List<HashMap> PressureList_2 = glService.querySupplyPressure1(paramsMap);
//			HashMap PressureMap_2 = PressureList_2.get(0);
//			 查询冷却板供水压力数据项：T_CUTOFF_RESULT_AVG_2
			List<HashMap> PressureList_3 = glService.querySupplyPressure2(paramsMap);
			HashMap PressureMap_3 = PressureList_3.get(0);
			// 查询热风压力数据项：T_CUTOFF_RESULT_AVG_9
			List<HashMap> PressureList_4 = glService.queryHotPressure(paramsMap);
			HashMap PressureMap_4 = PressureList_4.get(0);
			// 查询炉顶压力数据项：T_CUTOFF_RESULT_AVG_1
//			List<HashMap> PressureList_5 = glService.queryTopPressure(paramsMap);
//			HashMap PressureMap_5 = PressureList_5.get(0);
			// 声明PressureMap集合
			HashMap PressureMap = new HashMap<>();
			// 处理压力数据项到PressureMap集合
			PressureMap.putAll(PressureMap_1);
//			PressureMap.putAll(PressureMap_2);
			PressureMap.putAll(PressureMap_3);
			PressureMap.putAll(PressureMap_4);
//			PressureMap.putAll(PressureMap_5);
			// 获取压力对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("炉身静压数据项.xlsx");
			// 根据：数据库数据项、excel数据项，处理压力数据格式
			List<HashMap> FormatBlastFurnaceList = processService.formatBlastFurnaceData(PressureMap, BlastFurnaceList,resultId);
//			// 根据time：查询T_CUTOFF_RESULT表时间
//			Long timeStamp = processService.queryResultTimeStamp(1, resultId,"resultId");
//			HashMap timesParamsMap = new HashMap<>();
//			// 处理timeStamp到ThermalLoadRetMap集合
//			timesParamsMap.put("timeStamp", timeStamp);
//			FormatBlastFurnaceList.add(timesParamsMap);
			// 返回
			return JSON.toJSONString(FormatBlastFurnaceList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 *  查询压力历史趋势图数据项：炉身静压、冷却板供水压力、热风压力、炉顶压力
	 * 
	 * @remark1：传参rows：从当前时间查询rows行压力历史数据项
	 * @param rows
	 */
	@CrossOrigin
	@PostMapping("/queryPressureHistory")
	public String queryPressureHistory(@RequestParam(defaultValue = "60", required = false) Integer rows, String time) {
		try {
			// 声明PressureRetMap集合
			Map<Object, Object> PressureRetMap = new LinkedHashMap<>();
			// 设置参数
			HashMap paramsMap = new HashMap<>();
			paramsMap.put("rows", rows);
			paramsMap.put("time", time);
			// 查询炉身静压数据项：T_CUTOFF_RESULT_AVG_1
			List<HashMap> PressureList_1 = glService.queryStaticPressure(paramsMap);
			// 查询冷却板供水压力数据项：T_CUTOFF_RESULT_AVG_1
			List<HashMap> PressureList_2 = glService.querySupplyPressure1(paramsMap);
			// 查询冷却板供水压力数据项：T_CUTOFF_RESULT_AVG_2
			List<HashMap> PressureList_3 = glService.querySupplyPressure2(paramsMap);
			// 查询热风压力数据项：T_CUTOFF_RESULT_AVG_9
			List<HashMap> PressureList_4 = glService.queryHotPressure(paramsMap);
			// 查询炉顶压力数据项：T_CUTOFF_RESULT_AVG_1
			List<HashMap> PressureList_5 = glService.queryTopPressure(paramsMap);
			// 判断是否存在历史压力数据项 || 历史数据项长度是否大于rows
			if (PressureList_1.size() == 0 || PressureList_1.size() < rows) {
				return JSON.toJSONString(PressureRetMap);
			}
			// 声明PressureList集合
			List<HashMap> PressureList = new ArrayList<>();
			// 遍历压力数据项
			for (int i = 0; i < rows; i++) {
				// 获取压力数据项
				HashMap PressureMap_1 = PressureList_1.get(i);
				HashMap PressureMap_2 = PressureList_2.get(i);
				HashMap PressureMap_3 = PressureList_3.get(i);
				HashMap PressureMap_4 = PressureList_4.get(i);
				HashMap PressureMap_5 = PressureList_5.get(i);
				// 声明PressureMap集合
				HashMap PressureMap = new HashMap<>();
				// 处理压力数据项到PressureMap集合
				PressureMap.putAll(PressureMap_1);
				PressureMap.putAll(PressureMap_2);
				PressureMap.putAll(PressureMap_3);
				PressureMap.putAll(PressureMap_4);
				PressureMap.putAll(PressureMap_5);
				// 处理PressureMap集合到PressureList集合
				PressureList.add(PressureMap);
			}
			// 根据time：查询T_CUTOFF_RESULT表时间
			Long timeStamp = processService.queryResultTimeStamp(1, time);
			// 处理timeStamp到PressureRetMap集合
			PressureRetMap.put("timeStamp", timeStamp);
			// 获取压力对应的excel数据项
			List<BlastFurnaceMode> BlastFurnaceList = processService.readBlastFurnaceExcel("炉身静压数据项.xlsx");
			// 声明压力参数
			HashMap blastFurnaceParamsMap = new HashMap<>();
			blastFurnaceParamsMap.put("blastFurnaceList", BlastFurnaceList);
			blastFurnaceParamsMap.put("dataList", PressureList);
			blastFurnaceParamsMap.put("rows", rows);
			blastFurnaceParamsMap.put("type", 1);
			// 获取压力
			LinkedHashMap PressureDealMap = processService.dealBlastFurnaceHistory(blastFurnaceParamsMap);
			// 处理PressureDealMap到PressureRetMap集合
			PressureRetMap.putAll(PressureDealMap);
			// 返回
			return JSON.toJSONString(PressureRetMap);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询料层数据项
	 * 
	 * @remark1：传参resultId：进行历史回放查询功能
	 * @remark2：传参resultId：查询resultId对应的料层数据项
	 * @remark3：不传参resultId：查询最新的料层数据项
	 * @remark4：料层ht未做历史回放功能
	 * @param resultId
	 */
	@CrossOrigin
	@PostMapping("/queryMaterialLayer")
	public String queryMaterialLayer(Integer resultId) {
		try {
			List<HashMap> retMaterialLayerList = new ArrayList<>();
			// 声明参数
			HashMap paramsMap = resultId != null ? processService.getLayerClockByResultId(resultId, 6) : new HashMap<>();
			// 查询料层数据项
			List<HashMap> materialLayerList = glService.queryMaterialLayer(paramsMap);
			//
//			for (int i = 0; i < materialLayerList.size(); i += 2) {
				for (int i = 0; i < materialLayerList.size(); i += 2) {
				HashMap value1 = materialLayerList.get(i);
//				HashMap value2 = materialLayerList.get(i + 1);
				HashMap value3 = new HashMap<>();
				value3.put("BASEH", value1.get("BASEH"));
				value3.put("POINTX", value1.get("POINTX"));
				value3.put("LEVEL", value1.get("LEVEL"));
				value3.put("THICK", value1.get("THICK"));
				value3.put("THICK", value1.get("THICK"));
//				// 焦重
//				value3.put("WEIGHT1", value1.get("VALUE"));
//				// 矿重
//				value3.put("WEIGHT2", value2.get("VALUE"));
				//
				retMaterialLayerList.add(value3);
			}
			// 返回
			return JSON.toJSONString(retMaterialLayerList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}
	
	/**
	 * 查询料层数据项_1
	 * 
	 * @time:2022-11-03
	 * @remark1：查询最新的35层料层数据项
	 * @remark2：料层ht未做历史回放功能
	 */
	@CrossOrigin
	@PostMapping("/queryMaterialLayerData")
	public String queryMaterialLayerData() {
		try {
			// 查询料层数据项
			List<HashMap> dataList = glService.queryMaterialLayerData();
			// 返回
			return JSON.toJSONString(dataList);
		} catch (Exception e) {
			// 返回错误码
			return "500";
		}
	}

	/**
	 * 查询模型：侵蚀线数据，注：r-半径、z-高度、angle-角度
	 */
	@CrossOrigin
	@PostMapping("/queryErosionLine")
	public String queryErosionLine(Integer resultId) throws Exception {
		// 声明参数：startTime
		HashMap paramsMap = resultId != null ? processService.getErodeSolidByResultId(resultId, 7) : new HashMap<>();
		// 声明侵蚀线数据Map集合
		Map<Double, List<HashMap>> ErosionLineValue = new HashMap<>();
		// 查询侵蚀模型的角度数据总共22个角度对应的描述
		// {
		//	{
		//		"ANGLEID" : 1,
		//		"ANGLE" : null,
		//		"DESCR" : "Section 1:8.5"
		//	},
		//	{
		//		"ANGLEID" : 2,
		//		"ANGLE" : "5",
		//		"DESCR" : "Section 2:25.5"
		//	},
			List<HashMap> angleList = glService.queryErodeSolidAngle();
		// 声明emptyAngleList集合
		List<Double> emptyAngleList = new ArrayList<>();
		// 遍历角度集合
		for (int i = 0; i < angleList.size(); i++) {
			// 获取角度数据
			Map angleMap = angleList.get(i);
			//截取长度后为空
			String selectAngleValue = angleMap.get("DESCR").toString();
			Double putAngleValue=0.0;
			if(angleMap.get("ANGLE")!=""&&angleMap.get("ANGLE")!=null) {
				 putAngleValue = Double.valueOf(angleMap.get("ANGLE").toString());
			}
			// 添加角度参数
			paramsMap.put("angleValue", selectAngleValue);
			// 根据startTime、angleValue：获取calcId0
			HashMap calcIdMap = glService.queryErodeSolidCalcId(paramsMap);
			// 处理calcId到paramsMap集合
			paramsMap.put("calcId", calcIdMap.get("calcId"));
			/**
			 * 炉缸侵蚀历史数据逐步增加，queryErodeDataItem耗时逐步变长：待优化
			 */
			// 根据角度查询对应：高度和半径
			List<HashMap> ErosionLineList = glService.queryErodeDataItem(paramsMap);
			// 处理ErosionLineList集合到ErosionLineValue 向上取整和残厚侵蚀数据角度一致
			ErosionLineValue.put(Math.ceil(putAngleValue), ErosionLineList);
			// 判断ErosionLineList长度
			if (ErosionLineList.size() != 0) {
				emptyAngleList.add(putAngleValue);
			}
		}
		//根据本钢数据库新增代码
//		 排序
		Collections.sort(emptyAngleList);
		// 去重
		LinkedHashSet<Double> keySet = new LinkedHashSet<>(emptyAngleList);
		// 声明allRetMap集合
		Map<String, List<HashMap>> allRetMap = new LinkedHashMap<>();
		// 遍历keySet集合
		for (Double key : keySet) {
			// 根据key获取ErosionLineRetList集合
			List<HashMap> ErosionLineRetList = ErosionLineValue.get(key);
			// 按照 key1 进行排序
//
			ErosionLineRetList = ErosionLineRetList == null ? new ArrayList<>() : ErosionLineRetList;
			// 声明allRetList集合
			List<HashMap> allRetList = new ArrayList<>();
			allRetList.addAll(ErosionLineRetList);
			// 处理allRetList到allRetMap集合ceil
			// 格式化数字并输出结果
			allRetMap.put(String.valueOf(String.format("%.1f", Math.ceil(key))), allRetList);
		}
		return JSON.toJSONString(allRetMap);
/*
		-----------------------------------
*/
//		 获取侵蚀线key集合
//		List<Double> ErosionLineKeyList = new ArrayList(ErosionLineValue.keySet());
////		List<Double> ErosionLineKeyList = ErosionLineValue.keySet().stream()
////				.map(Math::ceil) // 使用 Math.ceil 方法向上取整
////				.collect(Collectors.toList());;
//		// 添加itemId参数
//		paramsMap.put("itemId", 65);
//		// 添加emptyAngleList参数
//		paramsMap.put("emptyAngleList", emptyAngleList);
//		// 获取其他角度侵蚀线集合
//		HashMap<Double, List<HashMap>> OtherAnglesLineMap = glService.dealOtherAnglesLine(paramsMap);
//		// 获取其他角度侵蚀线key集合（利用残厚计算侵蚀凝铁）
//		List<Double> OtherAnglesLineKeyList = new ArrayList(OtherAnglesLineMap.keySet());
////		// 查询T_MD_WR_POINT_VALUE表：侵蚀线数据项(本钢没有用到这个表)
////		HashMap<Double, List<HashMap>> MdWrPointValueMap = glService.dealMdWrPointValue();
//		// 获取T_MD_WR_POINT_VALUE表：侵蚀线key集合
////		List<Double> MdWrPointValueKeyList = new ArrayList(MdWrPointValueMap.keySet());
//		// 合并侵蚀线key集合(测试不合并key)
//		ErosionLineKeyList.addAll(OtherAnglesLineKeyList);
////		ErosionLineKeyList.addAll(MdWrPointValueKeyList);
//		// 排序
//		Collections.sort(ErosionLineKeyList);
//		// 去重
//		LinkedHashSet<Double> keySet = new LinkedHashSet<>(ErosionLineKeyList);
//		// 声明allRetMap集合
//		Map<String, List<HashMap>> allRetMap = new LinkedHashMap<>();
//		// 遍历keySet集合
//		for (Double key : keySet) {
//			// 根据key获取ErosionLineRetList集合
//			List<HashMap> ErosionLineRetList = ErosionLineValue.get(key);
//			ErosionLineRetList = ErosionLineRetList == null ? new ArrayList<>() : ErosionLineRetList;
//			// 根据key获取OtherAnglesLineRetList集合
//			List<HashMap> OtherAnglesLineRetList = OtherAnglesLineMap.get(key);
//			OtherAnglesLineRetList = OtherAnglesLineRetList == null ? new ArrayList<>() : OtherAnglesLineRetList;
//			// 根据key获取MdWrPointValueRetList集合
////			List<HashMap> MdWrPointValueRetList = MdWrPointValueMap.get(key);
////			MdWrPointValueRetList = MdWrPointValueRetList == null ? new ArrayList<>() : MdWrPointValueRetList;
//			// 声明allRetList集合
//			List<HashMap> allRetList = new ArrayList<>();
//			allRetList.addAll(ErosionLineRetList);
//			allRetList.addAll(OtherAnglesLineRetList);
////			allRetList.addAll(MdWrPointValueRetList);
//			// 处理allRetList到allRetMap集合
//			allRetMap.put(String.valueOf(key), allRetList);
//			// 格式化数字并输出结果  目前不用，排除Math.ceil()和数据库中的ROUND()函数的区别
////			allRetMap.put(String.valueOf(String.format("%.1f", key)), allRetList);
//		}
//		/**
//		 * 二次处理allRetMap数据集合：去除相同角度、标高：半径值较大数据项
//		 */
//		// 声明disAllRetMap集合
//		LinkedHashMap<String, Double> disAllRetMap = new LinkedHashMap<>();
//		// 遍历allRetMap集合
//		for (String angleKey : allRetMap.keySet()) {
//			List<HashMap> allRetList = allRetMap.get(angleKey);
//			// 遍历allRetList集合
//			for (int i = 0; i < allRetList.size(); i++) {
//				HashMap allRetHashMap = allRetList.get(i);
//				// 获取Z：标高
//				String Z = new BigDecimal(allRetHashMap.get("Z").toString()).stripTrailingZeros().toPlainString();
//				// 获取R：半径
//				Double R = Double.parseDouble(allRetHashMap.get("R").toString());
//				// 获取angleZ
//				String angleZ = angleKey + "@" + Z;
//				// 判断disAllRetMap.keySet
//				if (disAllRetMap.keySet().contains(angleZ)) {
//
//					// 判断disAllRetMap集合半径
//					if (disAllRetMap.get(angleZ) < R) {
//						disAllRetMap.put(angleZ, R);
//					}
//				} else {
//					disAllRetMap.put(angleZ, R);
//				}
//			}
//		}
//		// 声明disDealAllRetMap集合
//		LinkedHashMap<String, List> disDealAllRetMap = new LinkedHashMap<>();
//		for (String disKey : disAllRetMap.keySet()) {
//			String[] disKeyArrays = disKey.split("@");
//			// 获取角度：angle
//			String angle = disKeyArrays[0];
//			// 获取标高：Z
//			String Z = disKeyArrays[1];
//			// 获取R：半径
//			Double R = disAllRetMap.get(disKey);
//			// 根据angle：判断disValueList集合
//			List disValueList = disDealAllRetMap.get(angle) == null ? new ArrayList<>() : disDealAllRetMap.get(angle);
//			// 声明disValueHashMap集合
//			HashMap disValueHashMap = new HashMap<>();
//			// 处理Z到disValueHashMap集合
//			disValueHashMap.put("Z", Double.parseDouble(Z));
//			// 处理R到disValueHashMap集合
//			disValueHashMap.put("R", R);
//			// 处理disValueHashMap到disValueList集合
//			disValueList.add(disValueHashMap);
//			// 处理angle、disValueList到disDealAllRetMap集合
//			disDealAllRetMap.put(angle, disValueList);
//		}
//		LinkedHashMap<String, List<Map<Double, Double>>> disDealAllRetMapSort = new LinkedHashMap<>();
//		for (Map.Entry<String, List> entry : disDealAllRetMap.entrySet()) {
//			String key = entry.getKey();
//			List<Map<Double, Double>> values = entry.getValue();
//			// 使用Stream和Comparator进行排序
//			List<Map<Double, Double>> sortedValues = values.stream()
//					.sorted(Comparator.comparingDouble(map -> map.get("Z")))
//					.collect(Collectors.toList());;
//			// 更新排序后的值
//			disDealAllRetMapSort.put(key, sortedValues);
//		}
//		// 返回
//		return JSON.toJSONString(disDealAllRetMapSort);
	}

	/**
	 * 查询模型：凝固线数据，注：r-半径、z-高度、angle-角度
	 */
	@CrossOrigin
	@PostMapping("/querySolidificationLine")
	public String querySolidificationLine(Integer resultId) throws Exception {
		// 声明参数：startTime
		HashMap paramsMap = resultId != null ? processService.getErodeSolidByResultId(resultId, 7) : new HashMap<>();
		// 声明凝固线数据Map集合
		Map<Double, List<HashMap>> SolidificationLineValue = new HashMap<>();
		// 查询侵蚀模型的角度数据
		List<HashMap> angleList = glService.queryErodeSolidAngle();
		// 声明emptyAngleList集合
		List<Double> emptyAngleList = new ArrayList<>();
		// 遍历角度集合
		for (int i = 0; i < angleList.size(); i++) {
			// 获取角度数据
			Map angleMap = angleList.get(i);
			String selectAngleValue = angleMap.get("DESCR").toString();
			Double putAngleValue = Double.valueOf(angleMap.get("ANGLE").toString());
			// 添加角度参数
			paramsMap.put("angleValue", selectAngleValue);
			// 根据startTime、angleValue：获取calcId
			HashMap calcIdMap = glService.queryErodeSolidCalcId(paramsMap);
			// 处理calcId到paramsMap集合
			paramsMap.put("calcId", calcIdMap.get("calcId"));
			// 根据角度查询对应：高度和半径
			List<HashMap> SolidificationLineList = glService.querySolidDataItem(paramsMap);
			// 处理SolidificationLineList集合到SolidificationLineValue
			SolidificationLineValue.put(Math.ceil(putAngleValue), SolidificationLineList);
			// 判断SolidificationLineList长度
			if (SolidificationLineList.size() != 0) {
				emptyAngleList.add(putAngleValue);
			}
		}
//		//根据本钢数据库新增代码
//		 排序
		Collections.sort(emptyAngleList);
		// 去重
		LinkedHashSet<Double> keySet = new LinkedHashSet<>(emptyAngleList);
		// 声明allRetMap集合
		Map<String, List<HashMap>> allRetMap = new LinkedHashMap<>();
		// 遍历keySet集合
		for (Double key : keySet) {
			// 根据key获取ErosionLineRetList集合
			List<HashMap> ErosionLineRetList = SolidificationLineValue.get(key);
			// 按照 key1 进行排序
//
			ErosionLineRetList = ErosionLineRetList == null ? new ArrayList<>() : ErosionLineRetList;
			// 声明allRetList集合
			List<HashMap> allRetList = new ArrayList<>();
			allRetList.addAll(ErosionLineRetList);
			// 处理allRetList到allRetMap集合
			// 格式化数字并输出结果
			allRetMap.put(String.valueOf(String.format("%.1f", Math.ceil(key))), allRetList);
		}
		return JSON.toJSONString(allRetMap);
//		 获取凝固线key集合
//		List<Double> SolidificationLineKeyList = new ArrayList(SolidificationLineValue.keySet());
//		// 添加itemId参数
//		paramsMap.put("itemId", 50);
//		// 添加emptyAngleList参数
//		paramsMap.put("emptyAngleList", emptyAngleList);
//		// 获取其他角度凝固线集合
//		HashMap<Double, List<HashMap>> OtherAnglesLineMap = glService.dealOtherAnglesLine(paramsMap);
//		// 获取其他角度凝固线key集合
//		List<Double> OtherAnglesLineKeyList = new ArrayList(OtherAnglesLineMap.keySet());
//		// 合并凝固线key集合
//		SolidificationLineKeyList.addAll(OtherAnglesLineKeyList);
//		// 排序
//		Collections.sort(SolidificationLineKeyList);
//		// 去重
//		LinkedHashSet<Double> keySet = new LinkedHashSet<>(SolidificationLineKeyList);
//		// 声明allRetMap集合
//		Map<String, List<HashMap>> allRetMap = new LinkedHashMap();
//		// 遍历keySet集合
//		for (Double key : keySet) {
//			// 根据key获取SolidificationLineRetList集合
//			List<HashMap> SolidificationLineRetList = SolidificationLineValue.get(key);
//			SolidificationLineRetList = SolidificationLineRetList == null ? new ArrayList<>() : SolidificationLineRetList;
//			// 根据key获取OtherAnglesLineRetList集合
//			List<HashMap> OtherAnglesLineRetList = OtherAnglesLineMap.get(key);
//			OtherAnglesLineRetList = OtherAnglesLineRetList == null ? new ArrayList<>() : OtherAnglesLineRetList;
//			// 声明allRetList集合
//			List<HashMap> allRetList = new ArrayList<>();
//			allRetList.addAll(SolidificationLineRetList);
//			allRetList.addAll(OtherAnglesLineRetList);
//			// 处理allRetList到allRetMap集合
////			allRetMap.put(String.valueOf(key), allRetList);
//			// 格式化数字并输出结果
//			allRetMap.put(String.valueOf(String.format("%.1f", Math.ceil(key))), allRetList);
//		}
//		/**
//		 * 二次处理allRetMap数据集合：去除相同角度、标高：半径值较大数据项
//		 */
//		// 声明disAllRetMap集合
//		LinkedHashMap<String, Double> disAllRetMap = new LinkedHashMap<>();
//		// 遍历allRetMap集合
//		for (String angleKey : allRetMap.keySet()) {
//			List<HashMap> allRetList = allRetMap.get(angleKey);
//			// 遍历allRetList集合
//			for (int i = 0; i < allRetList.size(); i++) {
//				HashMap allRetHashMap = allRetList.get(i);
//				// 获取Z：标高
//				String Z = new BigDecimal(allRetHashMap.get("Z").toString()).stripTrailingZeros().toPlainString();
//				// 获取R：半径
//				Double R = Double.parseDouble(allRetHashMap.get("R").toString());
//				// 获取angleZ
//				String angleZ = angleKey + "@" + Z;
//				// 判断disAllRetMap.keySet
//				if (disAllRetMap.containsKey(angleZ)) {
//					// 判断disAllRetMap集合半径
//					if (disAllRetMap.get(angleZ) < R) {
//						disAllRetMap.put(angleZ, R);
//					}
//				} else {
//					disAllRetMap.put(angleZ, R);
//				}
//			}
//		}
//		// 声明disDealAllRetMap集合
//		LinkedHashMap<String, List> disDealAllRetMap = new LinkedHashMap<>();
//		for (String disKey : disAllRetMap.keySet()) {
//			String[] disKeyArrays = disKey.split("@");
//			// 获取角度：angle
//			String angle = disKeyArrays[0];
//			// 获取标高：Z
//			String Z = disKeyArrays[1];
//			// 获取R：半径
//			Double R = disAllRetMap.get(disKey);
//			// 根据angle：判断disValueList集合
//			List disValueList = disDealAllRetMap.get(angle) == null ? new ArrayList<>() : disDealAllRetMap.get(angle);
//			// 声明disValueHashMap集合
//			HashMap disValueHashMap = new HashMap<>();
//			// 处理Z到disValueHashMap集合
//			disValueHashMap.put("Z", Double.parseDouble(Z));
//			// 处理R到disValueHashMap集合
//			disValueHashMap.put("R", R);
//			// 处理disValueHashMap到disValueList集合
//			disValueList.add(disValueHashMap);
//			// 处理angle、disValueList到disDealAllRetMap集合
//			disDealAllRetMap.put(angle, disValueList);
//		}
//		LinkedHashMap<String, List<Map<Double, Double>>> disDealAllRetMapSort = new LinkedHashMap<>();
//		for (Map.Entry<String, List> entry : disDealAllRetMap.entrySet()) {
//			String key = entry.getKey();
//			List<Map<Double, Double>> values = entry.getValue();
//			// 使用Stream和Comparator进行排序
//			List<Map<Double, Double>> sortedValues = values.stream()
//					.sorted(Comparator.comparingDouble(map -> map.get("Z")))
//					.collect(Collectors.toList());;
//			// 更新排序后的值
//			disDealAllRetMapSort.put(key, sortedValues);
//		}
//		// 返回
//		return JSON.toJSONString(disDealAllRetMapSort);
	}

	/**
	 * 查询模型：残厚数据，注：r-半径、z-高度、angle-角度
	 */
	@CrossOrigin
	@PostMapping("/queryResidualThickness")
	public String queryResidualThickness(Integer resultId, Integer itemId) throws Exception {
		// 声明参数：startTime 取了前后各7天的时间来判断距离现在时间最近的一个时间就是侵蚀数据的时间
		HashMap paramsMap = resultId != null ? processService.getErodeSolidByResultId(resultId, 7) : new HashMap<>();
		// 添加itemId参数
		paramsMap.put("itemId", itemId);
		// 获取其他角度凝固线集合
		HashMap<Double, List<HashMap>> OtherAnglesLineMap = glService.dealResidualThickness(paramsMap);
		// 获取其他角度凝固线key集合
		List<Double> OtherAnglesLineKeyList = new ArrayList(OtherAnglesLineMap.keySet());
		// 排序
		Collections.sort(OtherAnglesLineKeyList);
		// 去重
		LinkedHashSet<Double> keySet = new LinkedHashSet<>(OtherAnglesLineKeyList);
		// 声明allRetMap集合
		Map<String, List<HashMap>> allRetMap = new LinkedHashMap();
		// 遍历keySet集合
		for (Double key : keySet) {
			// 根据key获取OtherAnglesLineRetList集合
			List<HashMap> OtherAnglesLineRetList = OtherAnglesLineMap.get(key);
			OtherAnglesLineRetList = OtherAnglesLineRetList == null ? new ArrayList<>() : OtherAnglesLineRetList;
			// 声明allRetList集合
			List<HashMap> allRetList = new ArrayList<>();
			allRetList.addAll(OtherAnglesLineRetList);
			// 处理allRetList到allRetMap集合
			allRetMap.put(String.valueOf(key), allRetList);
		}
		// 返回
		return JSON.toJSONString(allRetMap);
	}
	/**
	 * 查询模型：残厚数据历史趋势，注：r-半径、z-高度、angle-角度
	 */
	@CrossOrigin
	@PostMapping("/queryResidualThicknessHis")
	public String queryResidualThicknessHis( Integer rows, String time,Integer itemId,String high,String angle) throws Exception {
		// 声明参数：startTime 取了前后各7天的时间来判断距离现在时间最近的一个时间就是侵蚀数据的时间
//		HashMap paramsMap = resultId != null ? processService.getErodeSolidByResultId(resultId, 7) : new HashMap<>();
		HashMap paramsMap =  new HashMap<>();
		// 添加itemId参数
		paramsMap.put("startTime", time);
		paramsMap.put("itemId", itemId);
		paramsMap.put("rows", rows);
		paramsMap.put("high", high);
		paramsMap.put("angle", angle);
		// 获取其他角度凝固线集合
		HashMap<Object, List<HashMap>> OtherAnglesLineMap = glService.dealResidualThicknessHis(paramsMap);
		// 获取其他角度凝固线key集合
		List<String> OtherAnglesLineKeyList = new ArrayList(OtherAnglesLineMap.keySet());
		// 排序
		Collections.sort(OtherAnglesLineKeyList);
//		// 去重
//		LinkedHashSet<Double> keySet = new LinkedHashSet<>(OtherAnglesLineKeyList);
		// 声明allRetMap集合
		Map<String, List<HashMap>> allRetMap = new LinkedHashMap();
		// 遍历keySet集合
		for (Object key : OtherAnglesLineKeyList) {
			// 根据key获取OtherAnglesLineRetList集合
			List<HashMap> OtherAnglesLineRetList = OtherAnglesLineMap.get(key);
			OtherAnglesLineRetList = OtherAnglesLineRetList == null ? new ArrayList<>() : OtherAnglesLineRetList;
			// 声明allRetList集合
			List<HashMap> allRetList = new ArrayList<>();
			allRetList.addAll(OtherAnglesLineRetList);
			// 处理allRetList到allRetMap集合
			allRetMap.put(String.valueOf(key), allRetList);
		}
		// 返回
		return JSON.toJSONString(allRetMap);
	}
	@CrossOrigin
	@PostMapping("/queryResidualThicknessHisAll")
	public String queryResidualThicknessHisAll( Integer rows, String time,String high,String angle) throws Exception {
		// 声明参数：startTime 取了前后各7天的时间来判断距离现在时间最近的一个时间就是侵蚀数据的时间
//		HashMap paramsMap = resultId != null ? processService.getErodeSolidByResultId(resultId, 7) : new HashMap<>();
		HashMap paramsMap =  new HashMap<>();
		// 添加itemId参数
		paramsMap.put("slag", "all");
		paramsMap.put("startTime", time);
		paramsMap.put("itemId", 65);
		paramsMap.put("rows", rows);
		paramsMap.put("high", high);
		paramsMap.put("angle", angle);
		// 获取其他角度侵蚀线集合
		HashMap<Object, List<HashMap>> OtherAnglesLineMap = glService.dealResidualThicknessHis(paramsMap);
		// 获取其他角度凝固线key集合
		List<String> OtherAnglesLineKeyList = new ArrayList(OtherAnglesLineMap.keySet());
		paramsMap.put("itemId", 50);
		// 获取其他角度凝固线集合
		HashMap<Object, List<HashMap>> OtherAnglesLineMap2 = glService.dealResidualThicknessHis(paramsMap);
		// 获取其他角度凝固线key集合
		List<String> OtherAnglesLineKeyList2 = new ArrayList(OtherAnglesLineMap.keySet());
		// 排序
		Collections.sort(OtherAnglesLineKeyList);
//		// 去重
		LinkedHashSet<String> keySet = new LinkedHashSet<>(OtherAnglesLineKeyList);
		// 声明allRetMap集合
		Map<String, List<HashMap>> allRetMap = new LinkedHashMap();
		// 遍历keySet集合
		for (Object key : keySet) {
			// 根据key获取OtherAnglesLineRetList集合
			List<HashMap> OtherAnglesLineRetList = OtherAnglesLineMap.get(key);
			List<HashMap> OtherAnglesLineRetList2 = OtherAnglesLineMap2.get(key);
			OtherAnglesLineRetList = OtherAnglesLineRetList == null ? new ArrayList<>() : OtherAnglesLineRetList;
			OtherAnglesLineRetList2 = OtherAnglesLineRetList2 == null ? new ArrayList<>() : OtherAnglesLineRetList2;
			// 声明allRetList集合
			List<HashMap> allRetList = new ArrayList<>();
			allRetList.addAll(OtherAnglesLineRetList);
			allRetList.addAll(OtherAnglesLineRetList2);
			// 处理allRetList到allRetMap集合
			allRetMap.put(String.valueOf(key), allRetList);
		}
		// 声明disDealAllRetMap集合
		List disValueList=new ArrayList<>();
		LinkedHashMap<String, List> disDealAllRetMap = new LinkedHashMap<>();
		for (String disKey : allRetMap.keySet()) {
			String[] disKeyArrays = disKey.split("@");
			// 处理disValueHashMap到disValueList集合
			disValueList.add(allRetMap.get(disKey));
			// 处理angle、disValueList到disDealAllRetMap集合
			disDealAllRetMap.put(disKeyArrays[0]+"@"+disKeyArrays[1], disValueList);
		}
		// 返回
		return JSON.toJSONString(disDealAllRetMap);
	}
	/**
	 * 炉壳数据项
	 */
	@CrossOrigin
	@PostMapping("/queryMdErPolygon")
	public String queryMdErPolygon() throws Exception {
		HashMap paramsMap = new HashMap<>();
		List<HashMap> MdErPolygonList = glService.queryMdErPolygon(paramsMap);
		// 处理MdErPolygonList到angleMap集合
		HashMap angleMap = new HashMap<>();
		angleMap.put(0, MdErPolygonList);
		return JSON.toJSONString(angleMap);
	}

	/**
	 * 查询炉缸侵蚀耐火材数据项
	 */
	@CrossOrigin
	@PostMapping("/queryRefractoryMaterial")
	public String queryRefractoryMaterial() throws Exception {
		// 声明dataHashMap集合
		HashMap<String, List<HashMap>> dataHashMap = new HashMap<>();
		// 获取角度集合
		List<HashMap> angleList = glService.queryErodeSolidAngle();
		// 遍历angleList集合
		for (HashMap angleHashMap : angleList) {
			// 获取角度Id
			String angleId = String.valueOf(angleHashMap.get("ANGLEID"));
			// 获取角度
			String angle = String.valueOf(angleHashMap.get("ANGLE"));
			// 声明参数
			HashMap params = new HashMap<>();
			params.put("angleId", angleId);
			// 查询耐火材数据项
			List<HashMap> materialList = glService.queryRefractoryMaterial(params);
			System.out.println(angle);
			System.out.println(JSON.toJSONString(materialList));
			// 处理angle、materialList到dataHashMap集合
			dataHashMap.put(angle, materialList);
		}
		// 返回
		return JSON.toJSONString(dataHashMap);
	}
	@CrossOrigin
	@PostMapping("/query1")
	public void test() throws Exception {
		for (int i = 0; i <= 50; i++) {
			System.out.printf("%s",362.4-(7.2)*i);
		}
		Map<Object, Object> ThermocoupleRetMap = new LinkedHashMap<>();
		ThermocoupleRetMap.put("a","2023-08-01 15:30:30");
		ThermocoupleRetMap.put("b","0.622799");
		ThermocoupleRetMap.put("c","4.22");
		processService.JsonFileService(JSON.toJSONString(ThermocoupleRetMap));
//		String jsonp=processService.JsonReader("");
		Double a=0.622799;
		Double b=4.22;
		System.out.print(Double.parseDouble(ThermocoupleRetMap.get("b").toString())<Double.parseDouble(ThermocoupleRetMap.get("c").toString()));
		}
}