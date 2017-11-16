// Copyright (c) YugaByte, Inc.

package com.yugabyte.yw.commissioner.tasks.subtasks;

import com.yugabyte.yw.commissioner.Common;
import com.yugabyte.yw.common.ApiUtils;
import com.yugabyte.yw.common.ModelFactory;
import com.yugabyte.yw.common.NodeManager;
import com.yugabyte.yw.common.ShellProcessHandler;
import com.yugabyte.yw.models.AccessKey;
import com.yugabyte.yw.models.AvailabilityZone;
import com.yugabyte.yw.models.Provider;
import com.yugabyte.yw.models.Region;
import com.yugabyte.yw.models.Universe;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class AnsibleSetupServerTest extends NodeTaskBaseTest {
  private AnsibleSetupServer.Params createUniverse(Common.CloudType cloudType,
                                                   AccessKey.KeyInfo accessKeyInfo) {
    Provider p = ModelFactory.newProvider(defaultCustomer, cloudType);
    Region r = Region.create(p, "r-1", "r-1", "yb-image");
    AccessKey.create(p.uuid, "demo-key", accessKeyInfo);
    AvailabilityZone az = AvailabilityZone.create(r, "az-1", "az-1", "subnet-1");
    Universe u = Universe.create(cloudType.name() + "-universe", UUID.randomUUID(), defaultCustomer.getCustomerId());
    // Save the updates to the universe.
    Universe.saveDetails(u.universeUUID, ApiUtils.mockUniverseUpdater());
    AnsibleSetupServer.Params params = new AnsibleSetupServer.Params();
    params.azUuid = az.uuid;
    params.universeUUID = u.universeUUID;
    return params;
  }

  @Test
  public void testOnPremProviderWithAirGapOption() {
    when(mockNodeManager.nodeCommand(any(), any())).thenReturn(new ShellProcessHandler.ShellResponse());
    AnsibleSetupServer ansibleSetupServer = new AnsibleSetupServer();
    AccessKey.KeyInfo keyInfo = new AccessKey.KeyInfo();
    keyInfo.airGapInstall = true;
    AnsibleSetupServer.Params params = createUniverse(Common.CloudType.onprem, keyInfo);
    ansibleSetupServer.initialize(params);
    ansibleSetupServer.run();
    verify(mockNodeManager, times(0)).nodeCommand(NodeManager.NodeCommandType.Provision, params);
  }

  @Test
  public void testOnPremProviderWithPasswordlessOption() {
    when(mockNodeManager.nodeCommand(any(), any())).thenReturn(new ShellProcessHandler.ShellResponse());
    AnsibleSetupServer ansibleSetupServer = new AnsibleSetupServer();
    AccessKey.KeyInfo keyInfo = new AccessKey.KeyInfo();
    keyInfo.passwordlessSudoAccess = true;
    AnsibleSetupServer.Params params = createUniverse(Common.CloudType.onprem, keyInfo);
    ansibleSetupServer.initialize(params);
    ansibleSetupServer.run();
    verify(mockNodeManager, times(0)).nodeCommand(NodeManager.NodeCommandType.Provision, params);
  }

  @Test
  public void testOnPremProviderWithMultipleAccessKeys() {
    when(mockNodeManager.nodeCommand(any(), any())).thenReturn(new ShellProcessHandler.ShellResponse());
    AnsibleSetupServer ansibleSetupServer = new AnsibleSetupServer();
    AccessKey.KeyInfo keyInfo = new AccessKey.KeyInfo();
    keyInfo.passwordlessSudoAccess = true;
    AnsibleSetupServer.Params params = createUniverse(Common.CloudType.onprem, keyInfo);
    AccessKey.create(params.getProvider().uuid, "demo-key-2", keyInfo);
    ansibleSetupServer.initialize(params);
    ansibleSetupServer.run();
    verify(mockNodeManager, times(0)).nodeCommand(NodeManager.NodeCommandType.Provision, params);
  }

  @Test
  public void testAllProvidersWithAccessKey() {
    when(mockNodeManager.nodeCommand(any(), any())).thenReturn(new ShellProcessHandler.ShellResponse());
    for (Common.CloudType cloudType: Common.CloudType.values()) {
      AnsibleSetupServer ansibleSetupServer = new AnsibleSetupServer();
      AccessKey.KeyInfo keyInfo = new AccessKey.KeyInfo();
      AnsibleSetupServer.Params params = createUniverse(cloudType, keyInfo);
      ansibleSetupServer.initialize(params);
      ansibleSetupServer.run();
      verify(mockNodeManager, times(1)).nodeCommand(NodeManager.NodeCommandType.Provision, params);
    }
  }
}