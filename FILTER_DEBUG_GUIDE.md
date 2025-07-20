# 🔍 Filter Debug Guide - Complexity & Usage Filters

## 🐛 **Issue Reported**

The user reported that the following filters are not working:
- ❌ **Complexity Filter**: Selecting "All", "Simple", "Moderate", "Complex", "Very Complex"
- ❌ **Usage Filter**: Selecting "All", "Unused", "Rarely Used", "Used", "Frequently Used"

## 🔧 **Debug Logging Added**

I've added comprehensive debug logging to identify the root cause:

### **1. UI Controller Logging**
```kotlin
// In buildFilterCriteria()
println("VectorUIController: Complexity selection: '$complexitySelection'")
println("VectorUIController: Usage selection: '$usageSelection'")
println("VectorUIController: Built filter criteria - $criteria")

// In updateAdvancedFilter()
println("VectorUIController: Applying advanced filter - complexityLevel: ${criteria.complexityLevel}, usageStatus: ${criteria.usageStatus}")
```

### **2. Filter Implementation Logging**
```kotlin
// In DefaultVectorFilter.filter()
println("DefaultVectorFilter: Filtering ${items.size} vectors with criteria: $criteria")
println("DefaultVectorFilter: ${item.name} filtered out - complexity: ${item.analytics?.complexityLevel} (want: ${criteria.complexityLevel})")
println("DefaultVectorFilter: Filtered result: ${filtered.size} vectors")
```

## 🧪 **Testing Steps**

### **Test Complexity Filter:**
1. Open the plugin and wait for vectors to load
2. Go to **Advanced** tab
3. Change **Complexity** dropdown from "All" to "Simple"
4. **Check console output** for:
   ```
   VectorUIController: Complexity selection: 'Simple'
   VectorUIController: Built filter criteria - FilterCriteria(complexityLevel=SIMPLE, ...)
   DefaultVectorFilter: Filtering X vectors with criteria: FilterCriteria(complexityLevel=SIMPLE, ...)
   ```

### **Test Usage Filter:**
1. Change **Usage** dropdown from "All" to "Unused"
2. **Check console output** for:
   ```
   VectorUIController: Usage selection: 'Unused'
   VectorUIController: Built filter criteria - FilterCriteria(usageStatus=UNUSED, ...)
   DefaultVectorFilter: Filtering X vectors with criteria: FilterCriteria(usageStatus=UNUSED, ...)
   ```

## 🔍 **What to Look For**

### **Scenario 1: UI Not Triggering**
If you don't see any console output when changing dropdowns:
- **Problem**: Event listeners not attached to combo boxes
- **Solution**: Check if `view.comboComplexityFilter` and `view.comboUsageFilter` are null

### **Scenario 2: Wrong Selection Values**
If console shows unexpected values:
```
VectorUIController: Complexity selection: 'null'
VectorUIController: Usage selection: 'null'
```
- **Problem**: Combo box items not properly set or selected
- **Solution**: Check UI initialization in `VectorDrawablesView`

### **Scenario 3: Analytics Data Missing**
If filter shows vectors being filtered out due to null analytics:
```
DefaultVectorFilter: icon.xml filtered out - complexity: null (want: SIMPLE)
```
- **Problem**: Analytics not generated or not persisted properly
- **Solution**: Check analytics generation in loading process

### **Scenario 4: Filter Logic Issues**
If criteria are correct but filtering doesn't work:
```
DefaultVectorFilter: Filtering 10 vectors with criteria: FilterCriteria(complexityLevel=SIMPLE, ...)
DefaultVectorFilter: Filtered result: 10 vectors  // Should be fewer!
```
- **Problem**: Filter matching logic incorrect
- **Solution**: Check `matchesComplexityFilter` and `matchesUsageFilter` methods

## 🎯 **Expected Debug Output**

### **Working Complexity Filter:**
```
VectorUIController: Complexity selection: 'Simple'
VectorUIController: Built filter criteria - FilterCriteria(complexityLevel=SIMPLE, ...)
VectorUIController: Applying advanced filter - complexityLevel: SIMPLE, usageStatus: null
DefaultVectorFilter: Filtering 15 vectors with criteria: FilterCriteria(complexityLevel=SIMPLE, ...)
DefaultVectorFilter: icon_complex.xml filtered out - complexity: COMPLEX (want: SIMPLE)
DefaultVectorFilter: icon_moderate.xml filtered out - complexity: MODERATE (want: SIMPLE)
DefaultVectorFilter: Filtered result: 5 vectors
VectorUIController: Displaying 5 vectors
```

### **Working Usage Filter:**
```
VectorUIController: Usage selection: 'Unused'
VectorUIController: Built filter criteria - FilterCriteria(usageStatus=UNUSED, ...)
VectorUIController: Applying advanced filter - complexityLevel: null, usageStatus: UNUSED
DefaultVectorFilter: Filtering 15 vectors with criteria: FilterCriteria(usageStatus=UNUSED, ...)
DefaultVectorFilter: icon_used.xml filtered out - usage: USED (want: UNUSED)
DefaultVectorFilter: Filtered result: 8 vectors
VectorUIController: Displaying 8 vectors
```

## 🔧 **Potential Fixes**

Based on the debug output, we can apply targeted fixes:

1. **UI Issues**: Fix combo box initialization or event listeners
2. **Analytics Issues**: Fix analytics generation or persistence
3. **Filter Logic**: Fix matching logic in filter implementation
4. **Threading Issues**: Ensure analytics are available when filtering

---

**Next Steps**: Run the plugin, test the filters, and check console output to identify the specific issue! 