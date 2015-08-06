# spring-session-concurrency-control
spring session concurrency control base on spring session.

example:

inject configDataProvider:

`@Autowired
private ConfigDataProvider configDataProvider;`

in action set maximumSessions and principalAttr;  

add attribute to session:

`configDataProvider.setPrincipalAttr("principal");`

`session.setAttribute(
		configDataProvider.getPrincipalAttr(), account.getUsername());`  
