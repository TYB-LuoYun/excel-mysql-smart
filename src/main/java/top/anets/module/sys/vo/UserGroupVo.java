package top.anets.module.sys.vo;
import top.anets.module.sys.entity.UserGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class  UserGroupVo extends UserGroup{

}