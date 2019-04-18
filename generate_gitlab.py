#!/usr/bin/python
import os

fp=open('gitlab-ci.yml.template','r')
contents=fp.read()
fp.close()

platforms = {}
versions = {}

platforms['el6']={'dist':'el6', 'arch':'x86_64'}
platforms['el6:32']={'dist':'el6', 'arch':'i686'}
platforms['el7']={'dist':'el7', 'arch':'x86_64'}
#versions['2.0']={'latest_version':'develop-2-0', 'release_version':'$rh_20_release', 'short_version':'2.0', 'platform_keys':['el6', 'el6:32', 'el7']}
#versions['2.2']={'latest_version':'develop-2-2', 'release_version':'$rh_22_release', 'short_version':'2.2', 'platform_keys':['el6', 'el7']}
versions['2.3']={'latest_version':'develop-2-3', 'release_version':'$rh_23_release', 'short_version':'2.3', 'platform_keys':['el6', 'el7']}

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
base_component_dir = 'sdr/devices'
candidate_components = os.listdir(base_component_dir)
devices = []
for comp in candidate_components:
    if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.spd.xml'):
        components.append(comp)
        devices.append(comp)

base_component_dir = 'sdr/waveforms'
candidate_components = os.listdir(base_component_dir)
waveforms = []
for comp in candidate_components:
    if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.sad.xml'):
        waveforms.append(comp)

jobs = ''

package_template = """package:__DIST____V__:rh__SHORT_V__:__ASSET_NAME__:
  stage: __BUILD__
  variables:
    dist: __DIST__
    arch: __ARCH__
__NAMESPACE__
    uhd_repo: __UHDREPO__
    latest_version: __LATEST_V__
    release_version: __RELEASE_V__
    short_version: '__SHORT_V__'
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
__DEP__
  <<: *package

"""

create_template = """create-__DIST____V__:local:repos:
  stage: create_library_repo
  variables:
    dist: __DIST__
    short_version: '__SHORT_V__'
    arch: __ARCH__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

create_libraries_template = """create-__DIST____V__:local:libraries:repos:
  stage: create_repo
  variables:
    dist: __DIST__
    short_version: '__SHORT_V__'
    arch: __ARCH__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

create_comps_devs_template = """create-__DIST____V__:local:comps:devs:repos:
  stage: create_comps_devs_repo
  variables:
    dist: __DIST__
    short_version: '__SHORT_V__'
    arch: __ARCH__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

test_template = """test:__DIST____V__:rh__SHORT_V__:__ASSET_NAME__:
  variables:
    dist: __DIST__
    arch: __ARCH__
__NAMESPACE__
    uhd_repo: __UHDREPO__
    latest_version: __LATEST_V__
    release_version: __RELEASE_V__
    short_version: '__SHORT_V__'
    comp_type: __COMP_TYPE__
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
__DEP__
  <<: *test
"""

test_template_add = """  only:
    - branches
"""

deploy_template = """deploy-__DIST____V__-__SHORT_V__:__ASSET_NAME__:
  variables:
    dist: __DIST__
    arch: __ARCH__
    latest_version: __LATEST_V__
    release_version: __RELEASE_V__
    short_version: '__SHORT_V__'
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
  dependencies:
    - package:__DIST____V__:rh__SHORT_V__:__ASSET_NAME__
  <<: *s3

"""

def replace_package_template(os_version, rh_version, comp_name, base_library=False, isComponentOrDevice=False, isWaveform=False):
    retval = package_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())

    if "el6" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.7.3/$dist/$arch')
    elif "el7" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.9.4/$dist/$arch')
    if comp_name == "RX_Digitizer_Sim":
        retval = retval.replace('__NAMESPACE__\n', '    namespace: ""\n')
    else:
        retval = retval.replace('__NAMESPACE__\n', '')
    if base_library:
        retval = retval.replace('package', 'base_package')

    if base_library:
        retval = retval.replace("__DEP__\n", "")
    else:
        if isComponentOrDevice:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:libraries:repos\n")
        elif isWaveform:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:comps:devs:repos\n")
        else:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:repos\n")
    if isComponentOrDevice:
        retval = retval.replace("__BUILD__", "build_components")
    elif isWaveform:
        retval = retval.replace("__BUILD__", "build_waveforms")
    else:
        retval = retval.replace("__BUILD__", "build_libraries")

    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_test_template(os_version, rh_version, comp_name, branches=False, base_library=False, isComponent=True):
    retval = test_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())

    if "el6" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.7.3/$dist/$arch')
    elif "el7" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.9.4/$dist/$arch')
    if comp_name == "RX_Digitizer_Sim":
        retval = retval.replace('__NAMESPACE__\n', '    namespace: ""\n')
    else:
        retval = retval.replace('__NAMESPACE__\n', '')
    if isComponent:
        retval = retval.replace('__COMP_TYPE__', 'components')
    else:
        retval = retval.replace('__COMP_TYPE__', 'devices')
    if branches:
        retval += test_template_add
    retval += '\n'
    if base_library:
        retval = retval.replace("__DEP__\n", "")
    else:
        retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:libraries:repos\n")
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_deploy_template(os_version, rh_version, comp_name, base_library=False):
    retval = deploy_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())

    if base_library:
        retval = retval.replace('package', 'base_package')

    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_create_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'

    retval = create_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', platforms[os_version]['dist']).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_create_libraries_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'
        else:
            comp_dep_list += '    - package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'

    retval = create_libraries_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', platforms[os_version]['dist']).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_create_comps_devs_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'
        else:
            comp_dep_list += '    - package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'

    retval = create_comps_devs_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', platforms[os_version]['dist']).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

for _key in versions[next(iter(versions))]['platform_keys']:
    jobs += replace_create_template(_key, versions[next(iter(versions))]['short_version'], libraries)

for comp in libraries:
    base_package = False
    if comp == 'dsp':
        base_package = True
    rh_version = next(iter(versions))
    for os_version in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_package_template(os_version, rh_version, comp, base_package)
    for os_version in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_deploy_template(os_version, rh_version, comp, base_package)

for _key in versions[next(iter(versions))]['platform_keys']:
    jobs += replace_create_libraries_template(_key, versions[next(iter(versions))]['short_version'], libraries)

for comp in components:
    base_package = False
    isComponentOrDevice = True
    isComponent = True
    if comp in devices:
        isComponent = False

    rh_version = next(iter(versions))
    for os_version in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_package_template(os_version, rh_version, comp, base_package, isComponentOrDevice)

    if comp != 'MSDD':
        for os_version in versions[next(iter(versions))]['platform_keys']:
            jobs += replace_test_template(os_version, rh_version, comp, False, base_package, isComponent)

    for os_version in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_deploy_template(os_version, rh_version, comp, base_package)

comps_devs = libraries + components

for _key in versions[next(iter(versions))]['platform_keys']:
    jobs += replace_create_comps_devs_template(_key, versions[next(iter(versions))]['short_version'], comps_devs)

for comp in waveforms:
    base_package = False
    isComponent = False
    isWaveform = True

    rh_version = next(iter(versions))
    for os_version in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_package_template(os_version, rh_version, comp, base_package, isComponent, isWaveform)

    for os_version in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_deploy_template(os_version, rh_version, comp, base_package)

updated_contents = contents.replace('__JOBS__', jobs)

fp=open('.gitlab-ci.yml','w')
fp.write(updated_contents)
fp.close()
