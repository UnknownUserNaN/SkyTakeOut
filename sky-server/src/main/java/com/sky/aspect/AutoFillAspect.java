package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.apache.ibatis.ognl.OgnlRuntime.setFieldValue;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)") // 定义切入点表达式
    public void autoFillPointCut(){

    }

    /**
     * 前置通知，在方法执行前进行数据填充（自动为插入和更新的方法添加时间和操作人）
     *
     * @param joinPoint
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行自动的数据填充...");

        // 获取数据库操作方法的操作类型
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature(); // 获取到方法的签名对象
        AutoFill autoFill = methodSignature.getMethod().getAnnotation(AutoFill.class); // 获取到方法上的注解对象
        OperationType operationType = autoFill.value(); // 获取到该注解对象的操作属性

        // 获取到被拦截方法的参数（通常是实体对象）
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return; // 保险，防止参数为空
        }
        Object entity = args[0]; // 我们约定：插入和更新的方法，第一个参数就是实体对象

        // 获取到当前时间和当前操作的用户ID
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = BaseContext.getCurrentId();

        // 根据当前的操作类型，给对应的对象来赋值
        if(operationType == OperationType.INSERT){
            try {
                Method setCreateTime = entity.getClass().getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setCreateTime.invoke(entity, now);
                setUpdateTime.invoke(entity, now);
                setCreateUser.invoke(entity, operatorId);
                setUpdateUser.invoke(entity, operatorId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(operationType == OperationType.UPDATE) {
            try {
                Method setUpdateTime = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, operatorId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
