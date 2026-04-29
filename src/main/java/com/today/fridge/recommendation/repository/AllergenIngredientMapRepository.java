package com.today.fridge.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recommendation.entity.AllergenIngredientMap;
import java.util.List;


public interface AllergenIngredientMapRepository extends JpaRepository<AllergenIngredientMap, Long> {

	@Query("""
	select distinct a.ingredientName
	from AllergenIngredientMap a
	where a.allergenGroup.code in :codes
	and a.allergenGroup.isActive = true
	and a.isActive = true
	""")
	List<String> findIngredientNamesByAllergenCodes(
	    @Param("codes") List<String> codes
	);
}
