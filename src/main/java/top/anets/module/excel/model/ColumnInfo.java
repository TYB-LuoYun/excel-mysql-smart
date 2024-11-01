package top.anets.module.excel.model;

import lombok.Data;

@Data
public class ColumnInfo {
    private String columnName;
    private String dataType;
    private Integer length;
    private String comment;
    private String isNullable;

    private String  columnKey;


    /**
     * 对应excel列索引
     */
    private Integer excelColumnIndex;



    public boolean ifAllowNull(){
        if("NO".equalsIgnoreCase(isNullable)){
            return false;
        }
        return true;
    }

    public boolean ifPrimaryKey(){
        if("PRI".equalsIgnoreCase(columnKey)){
            return true;
        }
        return false;
    }

}
