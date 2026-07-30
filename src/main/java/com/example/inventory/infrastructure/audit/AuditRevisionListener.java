package com.example.inventory.infrastructure.audit;

import org.hibernate.envers.RevisionListener;

public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        if (revisionEntity instanceof AuditRevisionEntity auditRevisionEntity) {
            auditRevisionEntity.setUserName("system");
        }
    }
}
