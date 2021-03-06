package com.paypal.aurora

import com.paypal.aurora.model.OpenStackUser

class OpenStackUserService {
    private static final String USERS = 'users'
    private static final String TENANTS = 'tenants'
    private static final String ROLES = 'OS-KSADM/roles'

    def openStackRESTService

    def getAllUsers() {
        def resp = openStackRESTService.get(openStackRESTService.KEYSTONE, USERS)
        def users = []
        for (user in resp.users) {
            users << new OpenStackUser(user)
        }
        return users
    }

    def getAllUsersByTenant(def tenantId) {
        def resp = openStackRESTService.get(openStackRESTService.KEYSTONE, "$TENANTS/$tenantId/$USERS")
        def users = []
        for (user in resp.users) {
            users << new OpenStackUser(user)
        }
        return users
    }

    OpenStackUser getUserByName(def userName) {
        def resp = openStackRESTService.get(openStackRESTService.KEYSTONE, USERS, [name: userName])
        if (resp.user)
            return resp.user
        for (def user in resp.users) {
            if (user.name == userName) {
                return user
            }
        }
        return null
    }

    def getUserById(def userId) {
        def user = new OpenStackUser(openStackRESTService.get(openStackRESTService.KEYSTONE, "$USERS/$userId").user);
        return user
    }

    def getAllRoles() {
        def resp = openStackRESTService.get(openStackRESTService.KEYSTONE, ROLES)
        return resp.roles
    }

    def createUser(def user) {
        def body = [user: [email: user.email, password: user.password,
                name: user.name, tenantId: user.tenant_id, enabled: true]]
        def resp = openStackRESTService.post(openStackRESTService.KEYSTONE, USERS, body);
        setUserRole(body.user.tenantId, resp.user.id, user.role_id)
        return resp
    }

    def updateUser(def user) {
        def body = [user: [id: user.id, name: user.name, email: user.email]]

        if (user.password != "") {
            body.user << [password: user.password]
        }
        def resp = openStackRESTService.put(openStackRESTService.KEYSTONE, "$USERS/$body.user.id", null, body)
        openStackRESTService.put(openStackRESTService.KEYSTONE, "$USERS/$resp.user.id/", null,
                [user: [id: resp.user.id, tenantId: user.tenant_id]])
    }

    def deleteUserById(def userId) {
        openStackRESTService.delete(openStackRESTService.KEYSTONE, "$USERS/$userId")
    }

    def deleteUsersById(List<String> userIds) {
        return ServiceUtils.removeItems(this, "deleteUserById", userIds)
    }

    def getUserRoles(def userId, def tenantId) {
        def resp = openStackRESTService.get(openStackRESTService.KEYSTONE, "$TENANTS/$tenantId/$USERS/$userId/roles")
        return resp.roles
    }

    def setUserRole(def tenantId, def userId, def roleId) {
        def path = "$TENANTS/$tenantId";
        path += "/$USERS/$userId"
        path += "/roles/OS-KSADM/${roleId}"
        openStackRESTService.put(openStackRESTService.KEYSTONE, path, null);
    }

    def deleteUserRole(def tenantId, def userId, def roleId) {
        def path = "$TENANTS/$tenantId";
        path += "/$USERS/$userId"
        path += "/roles/OS-KSADM/${roleId}"
        openStackRESTService.delete(openStackRESTService.KEYSTONE, path);
    }

    def getUsersRoles(def users, def tenantId) {
        def userIdsToRoles = [:]
        for (OpenStackUser user in users) {
            userIdsToRoles[user.id] = []
            def roles = getUserRoles(user.id, tenantId)
            for (def role in roles) {
                def roleObj = new com.paypal.aurora.model.Role(role)
                if(roleObj.id) {
                    userIdsToRoles[user.id]<<roleObj
                }
            }
        }
        return userIdsToRoles
    }
}
