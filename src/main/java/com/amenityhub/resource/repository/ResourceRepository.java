package com.amenityhub.resource.repository;
import com.amenityhub.resource.entity.Resource;
import com.amenityhub.resource.entity.ResourceType;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByActiveTrue();

    List<Resource> findByResourceTypeAndActiveTrue(ResourceType resourceType);
}
