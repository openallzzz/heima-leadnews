package com.heima.apis.schedule;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.schedule.dtos.Task;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.PostConstruct;

@FeignClient("leadnews-schedule")
public interface IScheduleClient {

    /**
     * 添加延迟任务
     *
     * @param task
     * @return 返回任务在数据库中的id
     */
    @PostMapping("/api/v1/task/add")
    ResponseResult addTask(@RequestBody Task task);

    /**
     * 取消任务
     *
     * @param taskId
     * @return
     */
    @GetMapping("/api/v1/task/{taskId}")
    ResponseResult cancelTask(@PathVariable("taskId") long taskId);

    /**
     * 按照类型和优先级来拉取任务
     *
     * @param type
     * @param priority
     * @return
     */
    @GetMapping("/api/v1/task/{type}/{priority}")
    ResponseResult poll(@PathVariable("type") int type,
                     @PathVariable("proority") int priority);

}
