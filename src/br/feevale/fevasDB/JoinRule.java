package br.feevale.fevasDB;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JoinRule {
	public static enum JoinType { INNER, LEFT, RIGHT, FULL }

	public JoinType type() default JoinType.INNER;
	public String tableName();
	public String alias() default "";
	public String condition();
}