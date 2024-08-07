package top.anets.module.db.vo;
import top.anets.module.db.entity.AbaData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class  AbaDataVo extends AbaData{

}