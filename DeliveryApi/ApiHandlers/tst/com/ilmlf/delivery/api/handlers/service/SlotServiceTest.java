package com.ilmlf.delivery.api.handlers.service;


import com.ilmlf.delivery.api.handlers.util.DbUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SlotServiceTest {
  private SlotService slotService;
  private DbUtil dbUtilMock;
  private Connection connectionMock;

  @BeforeEach
  public void setUp() {
    this.dbUtilMock = Mockito.mock(DbUtil.class);
    this.connectionMock = Mockito.mock(Connection.class);
  }

  @Test
  public void connectionRetryWorks() {
    Mockito.when(this.dbUtilMock.createConnectionViaIamAuth(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(null, this.connectionMock);

    this.slotService = new SlotService(this.dbUtilMock);
    this.slotService.setCon(this.slotService.refreshDbConnection());

    assertNotNull(this.slotService.getCon());
  }
}
