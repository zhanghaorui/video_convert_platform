package com.fab.video_convert_platform.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * 患者访视信息值对象
 * 封装患者编码和访视点信息
 */
@Getter
@ToString
@EqualsAndHashCode
public class PatientVisit {
    
    private final String patientCode;
    private final String tpStage;

    /**
     * 私有构造函数
     */
    private PatientVisit(String patientCode, String tpStage) {
        this.patientCode = patientCode;
        this.tpStage = tpStage;
    }

    /**
     * 创建患者访视信息值对象
     * @param patientCode 患者编码
     * @param tpStage 访视点
     * @return 患者访视信息值对象
     */
    public static PatientVisit of(String patientCode, String tpStage) {
        validatePatientVisit(patientCode, tpStage);
        return new PatientVisit(patientCode, tpStage);
    }

    /**
     * 创建患者访视信息值对象（允许访视点为空，适用于MQ来源）
     * @param patientCode 患者编码
     * @param tpStage 访视点（可空）
     * @return 患者访视信息值对象
     */
    public static PatientVisit ofNullable(String patientCode, String tpStage) {
        validatePatientVisitNullable(patientCode, tpStage);
        return new PatientVisit(patientCode, tpStage);
    }

    /**
     * 验证患者访视信息
     */
    private static void validatePatientVisit(String patientCode, String tpStage) {
        if (Objects.isNull(patientCode) || patientCode.trim().isEmpty()) {
            throw new IllegalArgumentException("患者编码不能为空");
        }
        
        if (Objects.isNull(tpStage) || tpStage.trim().isEmpty()) {
            throw new IllegalArgumentException("访视点不能为空");
        }
        validatePatientCodeFormat(patientCode);
        validateTpStageFormat(tpStage);
    }

    /**
     * 验证患者访视信息（允许访视点为空）
     */
    private static void validatePatientVisitNullable(String patientCode, String tpStage) {
        if (Objects.isNull(patientCode) || patientCode.trim().isEmpty()) {
            throw new IllegalArgumentException("患者编码不能为空");
        }
        validatePatientCodeFormat(patientCode);
        validateTpStageFormat(tpStage); // 这个方法内部会处理null的情况
    }

    /**
     * 验证患者编码格式
     */
    private static void validatePatientCodeFormat(String patientCode) {
        // 患者编码格式验证：4-20位字母数字组合
        String trimmedPatientCode = patientCode.trim();
        if (trimmedPatientCode.length() < 4 || trimmedPatientCode.length() > 20) {
            throw new IllegalArgumentException("患者编码长度必须在4-20位之间");
        }
        
        if (!trimmedPatientCode.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("患者编码只能包含字母、数字、下划线和中划线");
        }
    }

    /**
     * 验证访视点格式
     */
    private static void validateTpStageFormat(String tpStage) {
        if (tpStage != null && !tpStage.trim().isEmpty()) {
            String trimmedTpStage = tpStage.trim();
            if (trimmedTpStage.length() > 50) {
                throw new IllegalArgumentException("访视点长度不能超过50位");
            }
        }
    }

    /**
     * 生成唯一标识符
     * 格式：patientCode_tpStage
     * @return 唯一标识符
     */
    public String getUniqueKey() {
        return patientCode + "_" + tpStage;
    }

    /**
     * 判断是否为基线访视
     * 业务规则：访视点为"BL"、"BASELINE"或"V0"时认为是基线访视
     * @return 是否为基线访视
     */
    public boolean isBaselineVisit() {
        String upperTpStage = tpStage.toUpperCase();
        return "BL".equals(upperTpStage) || 
               "BASELINE".equals(upperTpStage) || 
               "V0".equals(upperTpStage);
    }

    /**
     * 判断是否为随访访视
     * 业务规则：访视点以"V"开头且后面跟数字时认为是随访访视
     * @return 是否为随访访视
     */
    public boolean isFollowUpVisit() {
        return tpStage.toUpperCase().matches("^V\\d+$");
    }
}
