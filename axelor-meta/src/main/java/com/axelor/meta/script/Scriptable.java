package com.axelor.meta.script;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.axelor.auth.db.User;
import com.axelor.db.Model;
import com.axelor.rpc.Context;

public interface Scriptable<T extends Model> {

	Object __eval(T __self__, T __this__,  User __user__, Context __parent__, Model __ref__, LocalDate __date__, LocalDateTime __time__, DateTime __datetime__);
}
