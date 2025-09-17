package com.myrpc.leafe;

import com.myrpc.leafe.bootatrap.annotaion.MyrpcService;
import com.myrpc.leafe.proxy.proxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component      //BeanPostProcessor会在创建bean之后执行
public class MyrpcProxyBeanPostProcessors implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //想办法给有特定注解的字段创建代理对象
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            MyrpcService annotation = field.getAnnotation(MyrpcService.class);
            if(annotation != null){
                Class<?> type = field.getType();
                field.setAccessible( true);
                try {
                    field.set(bean, proxyFactory.getproxy(type));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            }

        }
        return bean;
    }
}
