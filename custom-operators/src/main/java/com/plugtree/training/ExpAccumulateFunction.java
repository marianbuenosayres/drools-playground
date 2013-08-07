package com.plugtree.training;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.kie.api.runtime.rule.AccumulateFunction;

public class ExpAccumulateFunction implements AccumulateFunction {

	public void writeExternal(ObjectOutput out) throws IOException {

	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

	}

	protected static class ExpData implements Externalizable {
        public double total = 0;

        public ExpData() {}

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            total   = in.readDouble();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeDouble(total);
        }

    }
	
	@Override
	public Serializable createContext() {
		return new ExpData();
	}

	@Override
	public void init(Serializable context) throws Exception {
		ExpData data = (ExpData) context;
        data.total = 1.0;
	}

	@Override
	public void accumulate(Serializable context, Object value) {
		ExpData data = (ExpData) context;
        data.total *= ((Number) value).doubleValue();
	}

	@Override
	public void reverse(Serializable context, Object value) throws Exception {
		ExpData data = (ExpData) context;
		data.total /= ((Number) value).doubleValue();
	}

	@Override
	public Object getResult(Serializable context) throws Exception {
		ExpData data = (ExpData) context;
		return new Double( data.total );
	}

	@Override
	public boolean supportsReverse() {
		return true;
	}

	@Override
	public Class<?> getResultType() {
		return Number.class;
	}

}
