package com.season.semiproject.spatial.accident;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccidentImportDAO {

    @Autowired
    private SqlSession session;

    public void deleteAccidentPointsBySourceFile(String sourceFile) {
        session.delete("deleteAccidentPointsBySourceFile", sourceFile);
    }

    public void insertAccidentPoint(AccidentPointInsertParam param) {
        session.insert("insertAccidentPoint", param);
    }
}
