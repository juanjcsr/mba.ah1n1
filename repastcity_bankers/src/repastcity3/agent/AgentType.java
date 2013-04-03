package repastcity3.agent;

public enum AgentType {

	WORKER("worker"),
	STUDENT("student"),
	KID("kid");
	
	private String type;
	
	private AgentType(String type){
		this.type = type;
	}
	
	@Override
	public String toString(){
		return this.type;
	}
}
