/**
 * 
 */
package org.testng.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;



/**
 * @author Rong Chen 4/3/2013
 * This is start to integrate TestNG with Jmeter. It is important that
 * not all functional test can apply to performance testing, however performance
 * tests are part of function tests. This annotation class will help to filter 
 * the list. we will figure out what more need to add to help jmeter performance 
 * test within TestNg framework.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface PerformanceTest {
	String info() default "";

}
