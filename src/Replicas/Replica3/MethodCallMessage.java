package Replicas.Replica3;

import java.io.Serializable;

public class MethodCallMessage implements Serializable {

	private static final long serialVersionUID = -4429834047832712376L;

	private String methodName;
	private Object[] arguments;

	public MethodCallMessage(String methodName, Object[] arguments) {
		this.methodName = methodName;
		this.arguments = arguments;
	}

	public String getMethodName() {
		return methodName;
	}

	public Object[] getArguments() {
		return arguments;
	}

}
