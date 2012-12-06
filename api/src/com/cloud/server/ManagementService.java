// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.server;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.alert.Alert;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.admin.host.command.ListHostsCmd;
import org.apache.cloudstack.api.admin.host.command.UpdateHostPasswordCmd;
import org.apache.cloudstack.api.admin.router.command.ListRoutersCmd;
import org.apache.cloudstack.api.user.address.command.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.user.config.command.ListCapabilitiesCmd;
import org.apache.cloudstack.api.user.guest.command.ListGuestOsCmd;
import org.apache.cloudstack.api.user.offering.command.ListDiskOfferingsCmd;
import org.apache.cloudstack.api.user.offering.command.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.user.ssh.command.CreateSSHKeyPairCmd;
import org.apache.cloudstack.api.user.ssh.command.DeleteSSHKeyPairCmd;
import org.apache.cloudstack.api.admin.systemvm.command.DestroySystemVmCmd;
import org.apache.cloudstack.api.user.volume.command.ExtractVolumeCmd;
import org.apache.cloudstack.api.user.template.command.ListTemplatesCmd;
import org.apache.cloudstack.api.user.template.command.UpdateTemplateCmd;
import org.apache.cloudstack.api.user.vm.command.GetVMPasswordCmd;
import com.cloud.api.commands.ListAlertsCmd;
import org.apache.cloudstack.api.user.job.command.ListAsyncJobsCmd;
import com.cloud.api.commands.ListCapacityCmd;
import org.apache.cloudstack.api.admin.config.command.ListCfgsByCmd;
import com.cloud.api.commands.ListClustersCmd;
import org.apache.cloudstack.api.user.event.command.ListEventsCmd;
import org.apache.cloudstack.api.user.guest.command.ListGuestOsCategoriesCmd;
import org.apache.cloudstack.api.user.iso.command.ListIsosCmd;
import com.cloud.api.commands.ListPodsByCmd;
import org.apache.cloudstack.api.user.ssh.command.ListSSHKeyPairsCmd;
import org.apache.cloudstack.api.admin.storagepool.command.ListStoragePoolsCmd;
import org.apache.cloudstack.api.admin.systemvm.command.ListSystemVMsCmd;
import org.apache.cloudstack.api.user.vmgroup.command.ListVMGroupsCmd;
import com.cloud.api.commands.ListVlanIpRangesCmd;
import org.apache.cloudstack.api.user.zone.command.ListZonesByCmd;
import org.apache.cloudstack.api.admin.systemvm.command.RebootSystemVmCmd;
import org.apache.cloudstack.api.user.ssh.command.RegisterSSHKeyPairCmd;
import org.apache.cloudstack.api.admin.systemvm.command.StopSystemVmCmd;
import org.apache.cloudstack.api.admin.domain.command.UpdateDomainCmd;
import org.apache.cloudstack.api.user.iso.command.UpdateIsoCmd;
import org.apache.cloudstack.api.user.vmgroup.command.UpdateVMGroupCmd;
import org.apache.cloudstack.api.admin.systemvm.command.UpgradeSystemVMCmd;
import com.cloud.api.commands.UploadCustomCertificateCmd;
import com.cloud.api.view.vo.DomainRouterJoinVO;
import com.cloud.async.AsyncJob;
import com.cloud.capacity.Capacity;
import com.cloud.configuration.Configuration;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.event.Event;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.network.IpAddress;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.StoragePool;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.SSHKeyPair;
import com.cloud.utils.Pair;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;

/**
 * Hopefully this is temporary.
 *
 */
public interface ManagementService {
    static final String Name = "management-server";

    /**
     * Retrieves the list of data centers with search criteria. Currently the only search criteria is "available" zones
     * for the
     * account that invokes the API. By specifying available=true all zones which the account can access. By specifying
     * available=false the zones where the account has virtual machine instances will be returned.
     *
     * @return a list of DataCenters
     */
    List<? extends DataCenter> listDataCenters(ListZonesByCmd cmd);

    /**
     * returns the a map of the names/values in the configuraton table
     *
     * @return map of configuration name/values
     */
    Pair<List<? extends Configuration>, Integer> searchForConfigurations(ListCfgsByCmd c);

    /**
     * Searches for Service Offerings by the specified search criteria Can search by: "name"
     *
     * @param cmd
     * @return List of ServiceOfferings
     */
    List<? extends ServiceOffering> searchForServiceOfferings(ListServiceOfferingsCmd cmd);

    /**
     * Searches for Clusters by the specified search criteria
     *
     * @param c
     * @return
     */
    Pair<List<? extends Cluster>, Integer> searchForClusters(ListClustersCmd c);

    /**
     * Searches for Clusters by the specified zone Id.
     * @param zoneId
     * @return
     */
    List<? extends Cluster> searchForClusters(long zoneId, Long startIndex, Long pageSizeVal, String hypervisorType);

    /**
     * Searches for Pods by the specified search criteria Can search by: pod name and/or zone name
     *
     * @param cmd
     * @return List of Pods
     */
    Pair<List<? extends Pod>, Integer> searchForPods(ListPodsByCmd cmd);

    /**
     * Searches for servers by the specified search criteria Can search by: "name", "type", "state", "dataCenterId",
     * "podId"
     *
     * @param cmd
     * @return List of Hosts
     */
    List<? extends Host> searchForServers(ListHostsCmd cmd);

    /**
     * Creates a new template
     *
     * @param cmd
     * @return updated template
     */
    VirtualMachineTemplate updateTemplate(UpdateIsoCmd cmd);

    VirtualMachineTemplate updateTemplate(UpdateTemplateCmd cmd);

    /**
     * Obtains a list of events by the specified search criteria. Can search by: "username", "type", "level",
     * "startDate",
     * "endDate"
     *
     * @param c
     * @return List of Events.
     */
    List<? extends Event> searchForEvents(ListEventsCmd c);

    /**
     * Obtains a list of routers by the specified search criteria. Can search by: "userId", "name", "state",
     * "dataCenterId",
     * "podId", "hostId"
     *
     * @param cmd
     * @return List of DomainRouters.
     */
    Pair<List<DomainRouterJoinVO>, Integer> searchForRouters(ListRoutersCmd cmd);

    /**
     * Obtains a list of IP Addresses by the specified search criteria. Can search by: "userId", "dataCenterId",
     * "address"
     *
     * @param cmd
     *            the command that wraps the search criteria
     * @return List of IPAddresses
     */
    Pair<List<? extends IpAddress>, Integer> searchForIPAddresses(ListPublicIpAddressesCmd cmd);

    /**
     * Obtains a list of all guest OS.
     *
     * @return list of GuestOS
     */
    Pair<List<? extends GuestOS>, Integer> listGuestOSByCriteria(ListGuestOsCmd cmd);

    /**
     * Obtains a list of all guest OS categories.
     *
     * @return list of GuestOSCategories
     */
    Pair<List<? extends GuestOsCategory>, Integer> listGuestOSCategoriesByCriteria(ListGuestOsCategoriesCmd cmd);

    VirtualMachine stopSystemVM(StopSystemVmCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException;

    VirtualMachine startSystemVM(long vmId);

    VirtualMachine rebootSystemVM(RebootSystemVmCmd cmd);

    VirtualMachine destroySystemVM(DestroySystemVmCmd cmd);

    VirtualMachine upgradeSystemVM(UpgradeSystemVMCmd cmd);

    /**
     * update an existing domain
     *
     * @param cmd
     *            - the command containing domainId and new domainName
     * @return Domain object if the command succeeded
     */
    Domain updateDomain(UpdateDomainCmd cmd);

    /**
     * Searches for alerts
     *
     * @param c
     * @return List of Alerts
     */
    Pair<List<? extends Alert>, Integer> searchForAlerts(ListAlertsCmd cmd);

    /**
     * list all the capacity rows in capacity operations table
     *
     * @param cmd
     * @return List of capacities
     */
    List<? extends Capacity> listCapacities(ListCapacityCmd cmd);

    /**
     * List ISOs that match the specified criteria.
     *
     * @param cmd
     *            The command that wraps the (optional) templateId, name, keyword, templateFilter, bootable, account,
     *            and zoneId
     *            parameters.
     * @return list of ISOs
     */
    Set<Pair<Long, Long>> listIsos(ListIsosCmd cmd);

    /**
     * List templates that match the specified criteria.
     *
     * @param cmd
     *            The command that wraps the (optional) templateId, name, keyword, templateFilter, bootable, account,
     *            and zoneId
     *            parameters.
     * @return list of ISOs
     */
    Set<Pair<Long, Long>> listTemplates(ListTemplatesCmd cmd);

    /**
     * Search for disk offerings based on search criteria
     *
     * @param cmd
     *            the command containing the criteria to use for searching for disk offerings
     * @return a list of disk offerings that match the given criteria
     */
    List<? extends DiskOffering> searchForDiskOfferings(ListDiskOfferingsCmd cmd);

    /**
     * List storage pools that match the given criteria
     *
     * @param cmd
     *            the command that wraps the search criteria (zone, pod, name, IP address, path, and cluster id)
     * @return a list of storage pools that match the given criteria
     */
    Pair<List<? extends StoragePool>, Integer> searchForStoragePools(ListStoragePoolsCmd cmd);

    /**
     * List system VMs by the given search criteria
     *
     * @param cmd
     *            the command that wraps the search criteria (host, name, state, type, zone, pod, and/or id)
     * @return the list of system vms that match the given criteria
     */
    Pair<List<? extends VirtualMachine>, Integer> searchForSystemVm(ListSystemVMsCmd cmd);

    /**
     * Returns back a SHA1 signed response
     *
     * @param userId
     *            -- id for the user
     * @return -- ArrayList of <CloudId+Signature>
     */
    ArrayList<String> getCloudIdentifierResponse(long userId);

    boolean updateHostPassword(UpdateHostPasswordCmd cmd);

    InstanceGroup updateVmGroup(UpdateVMGroupCmd cmd);

    Pair<List<? extends InstanceGroup>, Integer> searchForVmGroups(ListVMGroupsCmd cmd);

    Map<String, Object> listCapabilities(ListCapabilitiesCmd cmd);

    /**
     * Extracts the volume to a particular location.
     *
     * @param cmd
     *            the command specifying url (where the volume needs to be extracted to), zoneId (zone where the volume
     *            exists),
     *            id (the id of the volume)
     * @throws URISyntaxException
     * @throws InternalErrorException
     * @throws PermissionDeniedException
     *
     */
    Long extractVolume(ExtractVolumeCmd cmd) throws URISyntaxException;

    /**
     * return an array of available hypervisors
     *
     * @param zoneId
     *            TODO
     *
     * @return an array of available hypervisors in the cloud
     */
    List<String> getHypervisors(Long zoneId);

    /**
     * This method uploads a custom cert to the db, and patches every cpvm with it on the current ms
     *
     * @param cmd
     *            -- upload certificate cmd
     * @return -- returns a string on success
     * @throws ServerApiException
     *             -- even if one of the console proxy patching fails, we throw back this exception
     */
    String uploadCertificate(UploadCustomCertificateCmd cmd);

    String getVersion();

    /**
     * Searches for vlan by the specified search criteria Can search by: "id", "vlan", "name", "zoneID"
     *
     * @param cmd
     * @return List of Vlans
     */
    Pair<List<? extends Vlan>, Integer> searchForVlans(ListVlanIpRangesCmd cmd);

    /**
     * Search for async jobs by account and/or startDate
     *
     * @param cmd
     *            the command specifying the account and start date parameters
     * @return the list of async jobs that match the criteria
     */
    Pair<List<? extends AsyncJob>, Integer> searchForAsyncJobs(ListAsyncJobsCmd cmd);

    /**
     * Generates a random password that will be used (initially) by newly created and started virtual machines
     *
     * @return a random password
     */
    String generateRandomPassword();

    public Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId);

    public Long saveCompletedEvent(Long userId, Long accountId, String level, String type, String description, long startEventId);

    /**
     * Search registered key pairs for the logged in user.
     *
     * @param cmd
     *            The api command class.
     * @return The list of key pairs found.
     */
    Pair<List<? extends SSHKeyPair>, Integer> listSSHKeyPairs(ListSSHKeyPairsCmd cmd);

    /**
     * Registers a key pair for a given public key.
     *
     * @param cmd
     *            The api command class.
     * @return A VO with the key pair name and a finger print for the public key.
     */
    SSHKeyPair registerSSHKeyPair(RegisterSSHKeyPairCmd cmd);

    /**
     * Creates a new
     *
     * @param cmd
     *            The api command class.
     * @return A VO containing the key pair name, finger print for the public key and the private key material of the
     *         key pair.
     */
    SSHKeyPair createSSHKeyPair(CreateSSHKeyPairCmd cmd);

    /**
     * Deletes a key pair by name.
     *
     * @param cmd
     *            The api command class.
     * @return True on success. False otherwise.
     */
    boolean deleteSSHKeyPair(DeleteSSHKeyPairCmd cmd);

    /**
     * Finds and returns an encrypted password for a VM.
     *
     * @param cmd
     *            The api command class.
     * @return The encrypted password.
     */
    String getVMPassword(GetVMPasswordCmd cmd);

    Type findSystemVMTypeById(long instanceId);

    /**
     * List hosts for migrating the given VM. The API returns list of all hosts in the VM's cluster minus the current
     * host and
     * also a list of hosts that seem to have enough CPU and RAM capacity to host this VM.
     *
     * @param Long
     *            vmId
     *            Id of The VM to migrate
     * @return Pair<List<? extends Host>, List<? extends Host>> List of all Hosts in VM's cluster and list of Hosts with
     *         enough capacity
     */
    Pair<List<? extends Host>, List<? extends Host>> listHostsForMigrationOfVM(Long vmId, Long startIndex, Long pageSize);

    String[] listEventTypes();

    Pair<List<? extends HypervisorCapabilities>, Integer> listHypervisorCapabilities(Long id, HypervisorType hypervisorType, String keyword, Long startIndex, Long pageSizeVal);

    HypervisorCapabilities updateHypervisorCapabilities(Long id, Long maxGuestsLimit, Boolean securityGroupEnabled);

    /**
     * list all the top consumed resources across different capacity types
     *
     * @param cmd
     * @return List of capacities
     */
    List<? extends Capacity> listTopConsumedResources(ListCapacityCmd cmd);

}
