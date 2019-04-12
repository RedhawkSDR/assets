#!/usr/bin/python
import os

fp=open('gitlab-ci.yml.template','r')
contents=fp.read()
fp.close()

platforms = {}
versions = {}

platforms['el6']={'dist':'el6', 'arch':'x86_64'}
platforms['el6_32']={'dist':'el6', 'arch':'i686'}
platforms['el7']={'dist':'el7', 'arch':'x86_64'}
#versions['2.0']={'latest_version':'develop-2-0', 'release_version':'$rh_20_release', 'short_version':'2.0'}
versions['2.2']={'latest_version':'develop-2-2', 'release_version':'$rh_22_release', 'short_version':'2.2'}

base_component_dir = 'sdr/libraries'
candidate_components = os.listdir(base_component_dir)
libraries = []
for comp in candidate_components:
    if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.spd.xml'):
        libraries.append(comp)

base_component_dir = 'sdr/components'
candidate_components = os.listdir(base_component_dir)
components = []
for comp in candidate_components:
    if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.spd.xml'):
        components.append(comp)

jobs = ''

package_template = """package:__DIST__:rh__SHORT_V__:__ASSET_NAME__:
  stage: __BUILD__
  variables:
    dist: __DIST__
    arch: __ARCH__
    latest_version: __LATEST_V__
    release_version: __RELEASE_V__
    short_version: '__SHORT_V__'
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
__DEP__
  <<: *package

"""

create_template = """create-__DIST__:local:repos:
  stage: create_library_repo
  variables:
    dist: __DIST__
    short_version: '__SHORT_V__'
    arch: __ARCH__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

create_libraries_template = """create-__DIST__:local:libraries:repos:
  stage: create_repo
  variables:
    dist: __DIST__
    short_version: '__SHORT_V__'
    arch: __ARCH__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

test_template = """test:__DIST__:rh__SHORT_V__:__ASSET_NAME__:
  variables:
    dist: __DIST__
    arch: __ARCH__
    latest_version: __LATEST_V__
    release_version: __RELEASE_V__
    short_version: '__SHORT_V__'
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
__DEP__
  <<: *test
"""

test_template_add = """  only:
    - branches
"""

deploy_template = """deploy-__DIST__-__SHORT_V__:__ASSET_NAME__:
  variables:
    dist: __DIST__
    arch: __ARCH__
    latest_version: __LATEST_V__
    release_version: __RELEASE_V__
    short_version: '__SHORT_V__'
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
  dependencies:
    - package:__DIST__:rh__SHORT_V__:__ASSET_NAME__
  <<: *s3

"""

def replace_package_template(os_version, rh_version, comp_name, base_library=False, isComponent=False):
    retval = package_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())

    if base_library:
        retval = retval.replace('package', 'base_package')

    if base_library:
        retval = retval.replace("__DEP__\n", "")
    else:
        if isComponent:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+":local:libraries:repos\n")
        else:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+":local:repos\n")
    if isComponent:
        retval = retval.replace("__BUILD__", "build_components")
    else:
        retval = retval.replace("__BUILD__", "build_libraries")

    return retval

def replace_test_template(os_version, rh_version, comp_name, branches=False, base_library=False):
    retval = test_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())
    if branches:
        retval += test_template_add
    retval += '\n'
    if base_library:
        retval = retval.replace("__DEP__\n", "")
    else:
        retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+":local:repos\n")
    return retval

def replace_deploy_template(os_version, rh_version, comp_name, base_library=False):
    retval = deploy_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())

    if base_library:
        retval = retval.replace('package', 'base_package')

    return retval

def replace_create_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+':rh'+rh_version+':'+comp+'\n'

    retval = create_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', os_version).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    return retval

def replace_create_libraries_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+':rh'+rh_version+':'+comp+'\n'
        else:
            comp_dep_list += '    - package:'+platforms[os_version]['dist']+':rh'+rh_version+':'+comp+'\n'

    retval = create_libraries_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', os_version).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    return retval

jobs += replace_create_template('el6', '2.2', libraries)
jobs += replace_create_template('el7', '2.2', libraries)

for comp in libraries:
    base_package = False
    if comp == 'dsp':
        base_package = True
    if versions.has_key('2.0'):
        rh_version = '2.0'
        for os_version in ['el6', 'el6_32', 'el7']:
            jobs += replace_package_template(os_version, rh_version, comp, base_package)
    if versions.has_key('2.2'):
        os_version = 'el6'
        rh_version = '2.2'
        for os_version in ['el6', 'el7']:
            jobs += replace_package_template(os_version, rh_version, comp, base_package)

    if versions.has_key('2.0'):
        rh_version = '2.0'
        for os_version in ['el6', 'el6_32', 'el7']:
            jobs += replace_deploy_template(os_version, rh_version, comp, base_package)
    if versions.has_key('2.2'):
        os_version = 'el6'
        rh_version = '2.2'
        for os_version in ['el6', 'el7']:
            jobs += replace_deploy_template(os_version, rh_version, comp, base_package)

jobs += replace_create_libraries_template('el6', '2.2', libraries)
jobs += replace_create_libraries_template('el7', '2.2', libraries)

for comp in components:
    base_package = False
    isComponent = True
    if versions.has_key('2.0'):
        rh_version = '2.0'
        for os_version in ['el6', 'el6_32', 'el7']:
            jobs += replace_package_template(os_version, rh_version, comp, base_package, isComponent)
    if versions.has_key('2.2'):
        os_version = 'el6'
        rh_version = '2.2'
        for os_version in ['el6', 'el7']:
            jobs += replace_package_template(os_version, rh_version, comp, base_package, isComponent)

    if versions.has_key('2.0'):
        rh_version = '2.0'
        for os_version in ['el6', 'el6_32', 'el7']:
            jobs += replace_test_template(os_version, rh_version, comp, False, base_package)
    if versions.has_key('2.2'):
        os_version = 'el6'
        rh_version = '2.2'
        for os_version in ['el6', 'el7']:
            jobs += replace_test_template(os_version, rh_version, comp, False, base_package)

    if versions.has_key('2.0'):
        rh_version = '2.0'
        for os_version in ['el6', 'el6_32', 'el7']:
            jobs += replace_deploy_template(os_version, rh_version, comp, base_package)
    if versions.has_key('2.2'):
        os_version = 'el6'
        rh_version = '2.2'
        for os_version in ['el6', 'el7']:
            jobs += replace_deploy_template(os_version, rh_version, comp, base_package)

updated_contents = contents.replace('__JOBS__', jobs)

fp=open('.gitlab-ci.yml','w')
fp.write(updated_contents)
fp.close()
