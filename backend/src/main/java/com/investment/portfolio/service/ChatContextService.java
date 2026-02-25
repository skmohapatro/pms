package com.investment.portfolio.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

@Service
public class ChatContextService {

    @Autowired
    private EntityManager entityManager;

    public String buildDynamicContext() {
        StringBuilder context = new StringBuilder();
        context.append("You are an AI assistant for an Investment Portfolio Management System. ");
        context.append("Answer questions about the user's investment portfolio based on the following data.\n\n");

        Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

        for (EntityType<?> entityType : entities) {
            String entityName = entityType.getName();
            Class<?> javaType = entityType.getJavaType();
            
            context.append("=== ").append(formatEntityName(entityName)).append(" ===\n");
            context.append("Schema: ");
            context.append(getEntitySchema(javaType));
            context.append("\n");

            List<?> data = fetchEntityData(entityName);
            if (data != null && !data.isEmpty()) {
                context.append("Data (").append(data.size()).append(" records):\n");
                context.append(formatDataAsTable(data, javaType));
            } else {
                context.append("No data available.\n");
            }
            context.append("\n");
        }

        context.append("\nProvide helpful, accurate answers based on this portfolio data. ");
        context.append("When discussing investments, include specific numbers and calculations when relevant. ");
        context.append("Format currency values appropriately and be precise with quantities.");

        return context.toString();
    }

    private String formatEntityName(String entityName) {
        return entityName.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private String getEntitySchema(Class<?> entityClass) {
        StringBuilder schema = new StringBuilder();
        Field[] fields = entityClass.getDeclaredFields();
        List<String> fieldDescriptions = new ArrayList<>();

        for (Field field : fields) {
            if (!field.getName().startsWith("$") && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                String type = field.getType().getSimpleName();
                fieldDescriptions.add(field.getName() + " (" + type + ")");
            }
        }
        schema.append(String.join(", ", fieldDescriptions));
        return schema.toString();
    }

    @SuppressWarnings("unchecked")
    private List<?> fetchEntityData(String entityName) {
        try {
            String queryStr = "SELECT e FROM " + entityName + " e";
            return entityManager.createQuery(queryStr).getResultList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String formatDataAsTable(List<?> data, Class<?> entityClass) {
        if (data.isEmpty()) return "";

        StringBuilder table = new StringBuilder();
        Field[] fields = entityClass.getDeclaredFields();
        List<Field> validFields = new ArrayList<>();

        for (Field field : fields) {
            if (!field.getName().startsWith("$") && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                validFields.add(field);
            }
        }

        int maxRows = Math.min(data.size(), 100);

        for (int i = 0; i < maxRows; i++) {
            Object row = data.get(i);
            List<String> values = new ArrayList<>();
            for (Field field : validFields) {
                try {
                    Object value = field.get(row);
                    if (value instanceof Set) {
                        Set<?> set = (Set<?>) value;
                        values.add(field.getName() + ":[" + set.size() + " items]");
                    } else {
                        values.add(field.getName() + ":" + (value != null ? value.toString() : "null"));
                    }
                } catch (IllegalAccessException e) {
                    values.add(field.getName() + ":N/A");
                }
            }
            table.append("  ").append(String.join(", ", values)).append("\n");
        }

        if (data.size() > maxRows) {
            table.append("  ... and ").append(data.size() - maxRows).append(" more records\n");
        }

        return table.toString();
    }

    public Map<String, Object> getPortfolioSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            Long purchaseCount = (Long) entityManager
                .createQuery("SELECT COUNT(p) FROM PurchaseDateWise p")
                .getSingleResult();
            summary.put("totalPurchases", purchaseCount);

            Double totalInvestment = (Double) entityManager
                .createQuery("SELECT COALESCE(SUM(p.investment), 0) FROM PurchaseDateWise p")
                .getSingleResult();
            summary.put("totalInvestment", totalInvestment);

            Long companyCount = (Long) entityManager
                .createQuery("SELECT COUNT(c) FROM CompanyWiseAggregatedData c")
                .getSingleResult();
            summary.put("totalCompanies", companyCount);

            Long groupCount = (Long) entityManager
                .createQuery("SELECT COUNT(g) FROM InvestmentGroup g")
                .getSingleResult();
            summary.put("totalGroups", groupCount);

        } catch (Exception e) {
            summary.put("error", e.getMessage());
        }

        return summary;
    }
}
