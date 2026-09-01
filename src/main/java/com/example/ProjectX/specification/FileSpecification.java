package com.example.ProjectX.specification;

import org.springframework.data.jpa.domain.Specification;

import com.example.ProjectX.model.File;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class FileSpecification implements Specification<File> {
    private SearchCriteria criteria;

    public FileSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate (
            Root<File> root, 
            CriteriaQuery<?> query, 
            CriteriaBuilder criteriaBuilder
        ) {
        
            if (criteria.getOperation().equalsIgnoreCase(">")) {
                return criteriaBuilder.greaterThanOrEqualTo(
                    root.get(criteria.getKey()), 
                    Long.valueOf(criteria.getValue().toString())
                );
            } 
            
            if (criteria.getOperation().equalsIgnoreCase("<")) {
                return criteriaBuilder.lessThanOrEqualTo(
                    root.get(criteria.getKey()), 
                    Long.valueOf(criteria.getValue().toString())
                );
            }

            if (criteria.getOperation().equalsIgnoreCase(":")) {
                if (root.get(criteria.getKey()).getJavaType() == String.class) {
                    return criteriaBuilder.like(
                        root.get(criteria.getKey()), 
                        "%" + criteria.getValue() + "%");
                }
            }
            return null;
    }
}
