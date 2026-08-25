package com.example.inventory.infrastructure.audit;

import com.example.inventory.infrastructure.persistence.jpa.AuditRevisionEntity;
import org.hibernate.envers.RevisionListener;

public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        if (revisionEntity instanceof AuditRevisionEntity auditRevisionEntity) {
            auditRevisionEntity.setUserName("system");
        }
    }
}
