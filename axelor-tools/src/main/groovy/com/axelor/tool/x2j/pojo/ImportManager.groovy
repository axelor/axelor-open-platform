package com.axelor.tool.x2j.pojo

class ImportManager {
	
	private String base
	
	private Set<String> imports = new HashSet()
	
	private Map<String, String> names = new HashMap()
	
	private boolean groovy
	
	def GROOVY_PAT = ~/(java\.(io|net|lang|util))|(groovy\.(lang|util))/
	
	ImportManager(String base, boolean groovy) {
		this.base = base
		this.groovy = groovy
	}
	
	String importType(String fqn) {
		
		List<String> parts = fqn.split("\\.")
		if (parts.size() == 1)
			return fqn

		String simpleName = parts.pop()
		def name = simpleName
		
		if (name ==~ /[A-Z_]+/ && parts.last() ==~ /[A-Z_].*/) {
			simpleName = parts.pop()
			name = simpleName + "." + name
		}
		
		simpleName = simpleName.replaceAll("\\<.*", "")
		
		def pkg = parts.join(".")
		
		if (pkg == base || pkg == "java.lang") {
			return name
		}
		
		if (groovy && (pkg ==~ GROOVY_PAT || simpleName == 'BigDecimal')) {
			return name
		}
		
		def newFqn = pkg + "." + simpleName
		def canBeSimple = true
		
		if (names.containsKey(simpleName)) {
			def existing = names.get(simpleName)
			canBeSimple = existing == newFqn
		} else {
			names.put(simpleName, newFqn)
			imports.add(newFqn)
		}
		
		if (canBeSimple)
			return name
		return fqn
	}
	
	List<String> getImports() {
		def all = new ArrayList(imports)
		all.sort()
		return all
	}
}
