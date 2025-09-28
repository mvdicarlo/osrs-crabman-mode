package mvdicarlo.crabmanmode;

import java.util.List;

public interface UnlockedItemTableApi {
    UnlockedItemEntity getEntity(String id) throws Exception;
    List<UnlockedItemEntity> listEntities() throws Exception;
    void deleteEntity(String id) throws Exception;
    void insertEntity(UnlockedItemEntity entity) throws Exception;
}
