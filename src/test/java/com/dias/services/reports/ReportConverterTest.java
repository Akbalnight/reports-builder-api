package com.dias.services.reports;

import com.dias.services.reports.report.query.QueryDescriptor;
import org.junit.Assert;
import org.junit.Test;

public class ReportConverterTest {

    @Test
    public void restoreQueryDescriptor() {
        QueryDescriptor desc = Util.readObjectFromJSON("ReportConverter/queryDescriptor.json", QueryDescriptor.class);
        Assert.assertNotNull(desc);
        Assert.assertNotNull(desc.getAggregations());
        Assert.assertNotNull(desc.getSelect());
        Assert.assertNotNull(desc.getTable());
    }
}
